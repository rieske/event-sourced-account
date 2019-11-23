package lt.rieske.accounts.external;

import lombok.Data;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;


public class StressTests {

    private static String apiUrl() {
        return "http://localhost:8080/api";
    }

    @Data
    private static class TestCase {
        private final String name;
        private final TestMethod testMethod;
        private final int threadCount;
        private final int operationCount;
        private int conflicts;
        private long durationMillis;

        private String formatDurationSeconds() {
            return durationMillis / 1000.0 + "s";
        }

        private String throughput() {
            double opsPerSecond = (threadCount * operationCount) / (durationMillis / 1000.0);
            return opsPerSecond + " ops/s";
        }

        private void println() {
            System.out.printf("%-40s\t%-10s\t%-10s\t%-10s\t%-10s\t%-10s\n",
                    name, threadCount, operationCount, formatDurationSeconds(), conflicts, throughput());
        }

        void run() throws ExecutionException, InterruptedException {
            testMethod.run(this);
            println();
        }
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        canCreateAndQueryAccount();
        System.out.println("Sanity test passed\n");

        var threadCount = 8;
        var depositCount = 1000;

        System.out.printf("%-40s\t%-10s\t%-10s\t%-10s\t%-10s\t%-10s\n", "TEST NAME", "THREADS", "OPS", "DURATION", "CONFLICTS", "THROUGHPUT");
        for (int i = 0; i < 5; i++) {
            new TestCase("concurrentNonConflictingDeposits", StressTests::concurrentNonConflictingDeposits, threadCount, depositCount).run();
            new TestCase("concurrentIdempotentDeposits", StressTests::concurrentIdempotentDeposits, threadCount, depositCount).run();
            new TestCase("concurrentDistinctDeposits", StressTests::concurrentDistinctDeposits, threadCount, depositCount).run();
            System.out.println();
        }
    }

    private static void canCreateAndQueryAccount() {
        var ownerId = UUID.randomUUID();
        var accountId = newAccount(ownerId);

        given().baseUri(apiUrl())
                .when().get("/account/" + accountId)
                .then()
                .statusCode(200)
                .header("Content-Type", equalTo("application/json"))
                .body("accountId", equalTo(accountId.toString()))
                .body("ownerId", equalTo(ownerId.toString()))
                .body("balance", equalTo(0))
                .body("open", equalTo(true));
    }

    // N threads performing M deposits to N accounts.
    // Each thread "owns" an account - no concurrent modifications of an account - no conflicts
    private static void concurrentNonConflictingDeposits(TestCase testCase) throws InterruptedException, ExecutionException {
        var accountIds = new UUID[testCase.threadCount];
        for (int i = 0; i < testCase.threadCount; i++) {
            accountIds[i] = newAccount();
        }

        var executor = Executors.newFixedThreadPool(testCase.threadCount);
        testCase.durationMillis = measure(() -> {
            for (int i = 0; i < testCase.operationCount; i++) {
                var latch = new CountDownLatch(testCase.threadCount);
                for (int j = 0; j < testCase.threadCount; j++) {
                    int threadNo = j;
                    executor.submit(() -> {
                        try {
                            given().baseUri(apiUrl())
                                    .when().put("/account/" + accountIds[threadNo] + "/deposit?amount=1&transactionId=" + UUID.randomUUID())
                                    .then().statusCode(204);
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                        } finally {
                            latch.countDown();
                        }
                    });
                }
                latch.await();
            }
        });
        executor.shutdown();

        for (int i = 0; i < testCase.threadCount; i++) {
            assertOpenAccountWithBalance(accountIds[i], testCase.operationCount);
        }
    }

    // 1 account, N concurrent threads performing M deposits
    // for each deposit, each of N threads attempts the same transaction - expected amount - M
    private static void concurrentIdempotentDeposits(TestCase testCase) throws InterruptedException, ExecutionException {
        var accountId = newAccount();
        var executor = Executors.newFixedThreadPool(testCase.threadCount);

        Future<Integer>[] threadFutures = new Future[testCase.threadCount];

        testCase.durationMillis = measure(() -> {
            for (int i = 0; i < testCase.operationCount; i++) {
                var transactionId = UUID.randomUUID();
                for (int j = 0; j < testCase.threadCount; j++) {
                    threadFutures[j] = executor.submit(() -> withRetryOnConcurrentModification(() ->
                            given().baseUri(apiUrl())
                                    .when().put("/account/" + accountId + "/deposit?amount=1&transactionId=" + transactionId)
                                    .thenReturn().statusCode()));
                }
                for (int j = 0; j < testCase.threadCount; j++) {
                    testCase.conflicts += threadFutures[j].get();
                }
            }
        });
        executor.shutdown();

        assertOpenAccountWithBalance(accountId, testCase.operationCount);
    }

    // 1 account, N concurrent threads, each thread performing M deposits - expected amount - N*M
    private static void concurrentDistinctDeposits(TestCase testCase) throws InterruptedException, ExecutionException {
        var accountId = newAccount();
        var executor = Executors.newFixedThreadPool(testCase.threadCount);

        Future<Integer>[] threadFutures = new Future[testCase.threadCount];

        testCase.durationMillis = measure(() -> {
            for (int i = 0; i < testCase.operationCount; i++) {
                for (int j = 0; j < testCase.threadCount; j++) {
                    threadFutures[j] = executor.submit(() -> withRetryOnConcurrentModification(() ->
                            given().baseUri(apiUrl())
                                    .when().put("/account/" + accountId + "/deposit?amount=1&transactionId=" + UUID.randomUUID())
                                    .thenReturn().statusCode()));
                }
                for (int j = 0; j < testCase.threadCount; j++) {
                    testCase.conflicts += threadFutures[j].get();
                }
            }
        });
        executor.shutdown();

        assertOpenAccountWithBalance(accountId, testCase.operationCount * testCase.threadCount);
    }

    private static int withRetryOnConcurrentModification(Supplier<Integer> s) {
        int conflicts = 0;
        concurrentModificationRetryLoop:
        while (true) {
            int response = s.get();
            switch (response) {
                case 204:
                    break concurrentModificationRetryLoop;
                case 409:
                    conflicts++;
                    continue;
                default:
                    System.err.println("Unexpected response from server: " + response);
            }
        }
        return conflicts;
    }

    private static void assertOpenAccountWithBalance(UUID accountId, int balance) {
        given().baseUri(apiUrl())
                .when().get("/account/" + accountId)
                .then()
                .statusCode(200)
                .header("Content-Type", equalTo("application/json"))
                .body("accountId", equalTo(accountId.toString()))
                .body("balance", equalTo(balance))
                .body("open", equalTo(true));
    }

    private static UUID newAccount() {
        return newAccount(UUID.randomUUID());
    }

    private static UUID newAccount(UUID ownerId) {
        var accountId = UUID.randomUUID();
        given().baseUri(apiUrl())
                .when().post("/account/" + accountId + "?owner=" + ownerId)
                .then()
                .statusCode(201)
                .body(equalTo(""));
        return accountId;
    }

    private static long measure(InterruptingRunnable r) throws InterruptedException, ExecutionException {
        long startTime = System.currentTimeMillis();
        r.run();
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    @FunctionalInterface
    private interface InterruptingRunnable {
        void run() throws InterruptedException, ExecutionException;
    }

    @FunctionalInterface
    private interface TestMethod {
        void run(TestCase testCase) throws InterruptedException, ExecutionException;
    }
}
