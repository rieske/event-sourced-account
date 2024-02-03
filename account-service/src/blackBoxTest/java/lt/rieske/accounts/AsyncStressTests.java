package lt.rieske.accounts;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class AsyncStressTests {

    private static final AsyncAccountClient CLIENT = new AsyncAccountClient("http://localhost:8080/api");

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

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        canCreateAndQueryAccount();
        System.out.println("Sanity test passed\n");

        var threadCount = 8;
        var depositCount = 1000;

        System.out.printf("%-40s\t%-10s\t%-10s\t%-10s\t%-10s\t%-10s\n", "TEST NAME", "THREADS", "OPS", "DURATION", "CONFLICTS", "THROUGHPUT");
        for (int i = 0; i < 5; i++) {
            new TestCase("nonConflictingDeposits", AsyncStressTests::nonConflictingDeposits, threadCount, depositCount).run();
            new TestCase("idempotentDeposits", AsyncStressTests::idempotentDeposits, threadCount, depositCount).run();
            new TestCase("distinctDeposits", AsyncStressTests::distinctDeposits, threadCount, depositCount).run();
            System.out.println();
        }

        CLIENT.close();
    }

    private static void canCreateAndQueryAccount() throws ExecutionException, InterruptedException {
        var ownerId = UUID.randomUUID();
        var accountId = newAccount(ownerId);

        var future = new CompletableFuture<AsyncAccountClient.HttpResponse>();
        CLIENT.queryAccount(accountId, future);
        var response = future.get();
        assertThat(response.code()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{"
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

        testCase.durationMillis = measure(() -> {
            for (int i = 0; i < testCase.operationCount; i++) {
                @SuppressWarnings("unchecked")
                CompletableFuture<AsyncAccountClient.HttpResponse>[] threadFutures = new CompletableFuture[testCase.threadCount];

                for (int j = 0; j < testCase.threadCount; j++) {
                    var txId = UUID.randomUUID();
                    threadFutures[j] = new CompletableFuture<>();
                    CLIENT.deposit(accountIds[j], 1, txId, threadFutures[j]);
                }
                for (int j = 0; j < testCase.threadCount; j++) {
                    var response = threadFutures[j].get();
                    assertThat(response.code()).isEqualTo(204);
                    testCase.conflicts += response.conflicts();
                }
            }
        });

        for (int i = 0; i < testCase.threadCount; i++) {
            assertOpenAccountWithBalance(accountIds[i], testCase.operationCount);
        }
        assertThat(testCase.conflicts).isZero();
    }

    // 1 account, N concurrent threads performing M deposits
    // for each deposit, each of N threads attempts the same transaction - expected amount - M
    private static void idempotentDeposits(TestCase testCase) throws InterruptedException, ExecutionException {
        var accountId = newAccount();

        testCase.durationMillis = measure(() -> {
            for (int i = 0; i < testCase.operationCount; i++) {
                @SuppressWarnings("unchecked")
                CompletableFuture<AsyncAccountClient.HttpResponse>[] threadFutures = new CompletableFuture[testCase.threadCount];

                var txId = UUID.randomUUID();
                for (int j = 0; j < testCase.threadCount; j++) {
                    threadFutures[j] = new CompletableFuture<>();
                    CLIENT.deposit(accountId, 1, txId, threadFutures[j]);
                }
                for (int j = 0; j < testCase.threadCount; j++) {
                    var response = threadFutures[j].get();
                    assertThat(response.code()).isEqualTo(204);
                    testCase.conflicts += response.conflicts();
                }
            }
        });

        assertOpenAccountWithBalance(accountId, testCase.operationCount);
    }

    // 1 account, N concurrent threads, each thread performing M deposits - expected amount - N*M
    private static void distinctDeposits(TestCase testCase) throws InterruptedException, ExecutionException {
        var accountId = newAccount();

        testCase.durationMillis = measure(() -> {
            for (int i = 0; i < testCase.operationCount; i++) {
                @SuppressWarnings("unchecked")
                CompletableFuture<AsyncAccountClient.HttpResponse>[] threadFutures = new CompletableFuture[testCase.threadCount];

                for (int j = 0; j < testCase.threadCount; j++) {
                    threadFutures[j] = new CompletableFuture<>();
                    CLIENT.deposit(accountId, 1, threadFutures[j]);
                }
                for (int j = 0; j < testCase.threadCount; j++) {
                    var response = threadFutures[j].get();
                    assertThat(response.code()).isEqualTo(204);
                    testCase.conflicts += response.conflicts();
                }
            }
        });

        assertOpenAccountWithBalance(accountId, testCase.operationCount * testCase.threadCount);
    }

    private static void assertOpenAccountWithBalance(UUID accountId, int balance) throws ExecutionException, InterruptedException {
        var future = new CompletableFuture<AsyncAccountClient.HttpResponse>();
        CLIENT.queryAccount(accountId, future);
        var response = future.get();
        assertThat(response.code()).isEqualTo(200);
        var accountJson = response.body();
        assertThat(accountJson).contains("\"accountId\":\"" + accountId + "\"");
        assertThat(accountJson).contains("\"balance\":" + balance);
        assertThat(accountJson).contains("\"open\":true");
    }

    private static UUID newAccount() throws ExecutionException, InterruptedException {
        return newAccount(UUID.randomUUID());
    }

    private static UUID newAccount(UUID ownerId) throws ExecutionException, InterruptedException {
        var accountId = UUID.randomUUID();
        var future = new CompletableFuture<AsyncAccountClient.HttpResponse>();
        CLIENT.openAccount(accountId, ownerId, future);
        var response = future.get();
        assertThat(response.code()).isEqualTo(201);
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
