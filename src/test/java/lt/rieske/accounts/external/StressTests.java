package lt.rieske.accounts.external;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

// External tests meant to be run against an already spawned service for experimentation purposes
public class StressTests {

    private static String apiUrl() {
        return "http://localhost:8080/api";
    }

    public static void main(String[] args) throws InterruptedException {
        canCreateAndQueryAccount();
        System.out.println("Sanity test passed\n");

        var depositCount = 100;
        var threadCount = 8;

        System.out.printf("%-40s\t%s\n", "TEST NAME", "DURATION");
        for (int i = 0; i < 5; i++) {
            var d1 = concurrentNonConflictingDeposits(threadCount, depositCount);
            System.out.printf("%-40s\t%ss\n", "concurrentNonConflictingDeposits", d1.toMillis() / 1000.0);
            var d2 = concurrentIdempotentDeposits(threadCount, depositCount);
            System.out.printf("%-40s\t%ss\n", "concurrentIdempotentDeposits", d2.toMillis() / 1000.0);
            var d3 = concurrentDistinctDeposits(threadCount, depositCount);
            System.out.printf("%-40s\t%ss\n", "concurrentDistinctDeposits", d3.toMillis() / 1000.0);
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
    private static Duration concurrentNonConflictingDeposits(int threadCount, int depositCount) throws InterruptedException {
        var accountIds = new UUID[threadCount];
        for (int i = 0; i < threadCount; i++) {
            accountIds[i] = newAccount();
        }

        var executor = Executors.newFixedThreadPool(threadCount);
        var testDuration = measure(() -> {
            for (int i = 0; i < depositCount; i++) {
                var latch = new CountDownLatch(threadCount);
                for (int j = 0; j < threadCount; j++) {
                    int threadNo = j;
                    executor.submit(() -> {
                        given().baseUri(apiUrl())
                                .when().put("/account/" + accountIds[threadNo] + "/deposit?amount=1&transactionId=" + UUID.randomUUID())
                                .then().statusCode(204);
                        latch.countDown();
                    });
                }
                latch.await();
            }
        });
        executor.shutdown();

        for (int i = 0; i < threadCount; i++) {
            assertOpenAccountWithBalance(accountIds[i], depositCount);
        }

        return testDuration;
    }

    // 1 account, N concurrent threads performing M deposits
    // for each deposit, each of N threads attempts the same transaction - expected amount - M
    private static Duration concurrentIdempotentDeposits(int threadCount, int depositCount) throws InterruptedException {
        var accountId = newAccount();
        var executor = Executors.newFixedThreadPool(threadCount);

        var testDuration = measure(() -> {
            for (int i = 0; i < depositCount; i++) {
                var latch = new CountDownLatch(threadCount);
                var transactionId = UUID.randomUUID();
                for (int j = 0; j < threadCount; j++) {
                    executor.submit(() -> {
                        withRetryOnConcurrentModification(() ->
                                given().baseUri(apiUrl())
                                        .when().put("/account/" + accountId + "/deposit?amount=1&transactionId=" + transactionId)
                                        .thenReturn().statusCode());
                        latch.countDown();
                    });
                }
                latch.await();
            }
        });
        executor.shutdown();

        assertOpenAccountWithBalance(accountId, depositCount);
        return testDuration;
    }

    // 1 account, N concurrent threads, each thread performing M deposits - expected amount - N*M
    private static Duration concurrentDistinctDeposits(int threadCount, int depositCount) throws InterruptedException {
        var accountId = newAccount();
        var executor = Executors.newFixedThreadPool(threadCount);

        var testDuration = measure(() -> {
            for (int i = 0; i < depositCount; i++) {
                var latch = new CountDownLatch(threadCount);
                for (int j = 0; j < threadCount; j++) {
                    executor.submit(() -> {
                        withRetryOnConcurrentModification(() ->
                                given().baseUri(apiUrl())
                                        .when().put("/account/" + accountId + "/deposit?amount=1&transactionId=" + UUID.randomUUID())
                                        .thenReturn().statusCode());
                        latch.countDown();
                    });
                }
                latch.await();
            }
        });
        executor.shutdown();

        assertOpenAccountWithBalance(accountId, depositCount * threadCount);

        return testDuration;
    }

    private static void withRetryOnConcurrentModification(Supplier<Integer> s) {
        concurrentModificationRetryLoop:
        while (true) {
            int response = s.get();
            switch (response) {
                case 204:
                    break concurrentModificationRetryLoop;
                case 409:
                    continue;
                default:
                    throw new RuntimeException("Unexpected response: " + response);
            }
        }
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

    private static Duration measure(InterruptingRunnable r) throws InterruptedException {
        long startTime = System.nanoTime();
        r.run();
        long endTime = System.nanoTime();
        return Duration.ofNanos(endTime - startTime);
    }

    @FunctionalInterface
    private interface InterruptingRunnable {
        void run() throws InterruptedException;
    }
}
