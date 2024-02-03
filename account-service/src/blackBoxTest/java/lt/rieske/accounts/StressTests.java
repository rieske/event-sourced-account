package lt.rieske.accounts;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class StressTests {

    private static final AccountClient CLIENT = new AccountClient("http://localhost:8080/api");

    private static class TestCase {
        private final String name;
        private final TestMethod testMethod;
        private final int threadCount;
        private final int operationCount;
        private int conflicts;
        private long durationMillis;

        private TestCase(String name, TestMethod testMethod, int threadCount, int operationCount) {
            this.name = name;
            this.testMethod = testMethod;
            this.threadCount = threadCount;
            this.operationCount = operationCount;
        }

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
            new TestCase("nonConflictingDeposits", StressTests::nonConflictingDeposits, threadCount, depositCount).run();
            new TestCase("idempotentDeposits", StressTests::idempotentDeposits, threadCount, depositCount).run();
            new TestCase("distinctDeposits", StressTests::distinctDeposits, threadCount, depositCount).run();
            System.out.println();
        }
    }

    private static void canCreateAndQueryAccount() {
        var ownerId = UUID.randomUUID();
        var accountId = newAccount(ownerId);

        var accountJson = CLIENT.queryAccount(accountId);
        assertThat(accountJson).isEqualTo("{"
                + "\"accountId\":\"" + accountId + "\","
                + "\"ownerId\":\"" + ownerId + "\","
                + "\"balance\":0,"
                + "\"open\":true"
                + "}");
    }

    // N threads performing M deposits to N accounts.
    // Each thread "owns" an account - no concurrent modifications of an account - no conflicts
    private static void nonConflictingDeposits(TestCase testCase) throws InterruptedException, ExecutionException {
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
                    var txId = UUID.randomUUID();
                    executor.submit(() -> {
                        while (true) {
                            try {
                                int status = CLIENT.deposit(accountIds[threadNo], 1, txId);
                                assertThat(status).isEqualTo(204);
                            } catch (Exception e) {
                                System.err.println(e.getMessage());
                                continue;
                            }
                            break;
                        }
                        latch.countDown();
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
    private static void idempotentDeposits(TestCase testCase) throws InterruptedException, ExecutionException {
        var accountId = newAccount();
        var executor = Executors.newFixedThreadPool(testCase.threadCount);

        @SuppressWarnings("unchecked")
        Future<Integer>[] threadFutures = new Future[testCase.threadCount];

        testCase.durationMillis = measure(() -> {
            for (int i = 0; i < testCase.operationCount; i++) {
                var txId = UUID.randomUUID();
                for (int j = 0; j < testCase.threadCount; j++) {
                    threadFutures[j] = executor.submit(() -> withRetryOnConflict(() -> CLIENT.deposit(accountId, 1, txId)));
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
    private static void distinctDeposits(TestCase testCase) throws InterruptedException, ExecutionException {
        var accountId = newAccount();
        var executor = Executors.newFixedThreadPool(testCase.threadCount);

        @SuppressWarnings("unchecked")
        Future<Integer>[] threadFutures = new Future[testCase.threadCount];

        testCase.durationMillis = measure(() -> {
            for (int i = 0; i < testCase.operationCount; i++) {
                for (int j = 0; j < testCase.threadCount; j++) {
                    var txId = UUID.randomUUID();
                    threadFutures[j] = executor.submit(() -> withRetryOnConflict(() -> CLIENT.deposit(accountId, 1, txId)));
                }
                for (int j = 0; j < testCase.threadCount; j++) {
                    testCase.conflicts += threadFutures[j].get();
                }
            }
        });
        executor.shutdown();

        assertOpenAccountWithBalance(accountId, testCase.operationCount * testCase.threadCount);
    }

    private static int withRetryOnConflict(Supplier<Integer> s) {
        int conflicts = 0;
        concurrentModificationRetryLoop:
        while (true) {
            try {
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
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
        return conflicts;
    }

    private static void assertOpenAccountWithBalance(UUID accountId, int balance) {
        var accountJson = CLIENT.queryAccount(accountId);
        assertThat(accountJson).contains("\"accountId\":\"" + accountId + "\"");
        assertThat(accountJson).contains("\"balance\":" + balance);
        assertThat(accountJson).contains("\"open\":true");
    }

    private static UUID newAccount() {
        return newAccount(UUID.randomUUID());
    }

    private static UUID newAccount(UUID ownerId) {
        var accountId = UUID.randomUUID();
        CLIENT.openAccount(accountId, ownerId);
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
