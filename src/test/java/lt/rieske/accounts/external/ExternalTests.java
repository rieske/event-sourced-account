package lt.rieske.accounts.external;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

// External tests meant to be run against an already spawned service for experimentation purposes
@Tag("external")
@Slf4j
class ExternalTests {

    private static String apiUrl() {
        return "http://localhost:8080/api";
    }

    @Test
    void canCreateAndQueryAccount() {
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
    @Test
    void concurrentNonConflictingDeposits() throws InterruptedException {
        var depositCount = 500;
        var threadCount = 8;
        var executor = Executors.newFixedThreadPool(threadCount);

        var accountIds = new UUID[threadCount];
        for (int i = 0; i < threadCount; i++) {
            accountIds[i] = newAccount();
        }

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
        executor.shutdown();

        for (int i = 0; i < threadCount; i++) {
            assertOpenAccountWithBalance(accountIds[i], depositCount);
        }
    }

    // 1 account, N concurrent threads performing M deposits
    // for each deposit, each of N threads attempts the same transaction - expected amount - M
    @Test
    void concurrentIdempotentDeposits() throws InterruptedException {
        var accountId = newAccount();

        var depositCount = 500;
        var threadCount = 8;
        var executor = Executors.newFixedThreadPool(threadCount);

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
        executor.shutdown();

        assertOpenAccountWithBalance(accountId, depositCount);
    }

    // 1 account, N concurrent threads, each thread performing M deposits - expected amount - N*M
    @Test
    void concurrentDistinctDeposits() throws InterruptedException {
        var accountId = newAccount();

        var depositCount = 500;
        var threadCount = 8;
        var executor = Executors.newFixedThreadPool(threadCount);

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
        executor.shutdown();

        assertOpenAccountWithBalance(accountId, depositCount*threadCount);
    }

    private void withRetryOnConcurrentModification(Supplier<Integer> s) {
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

    private void assertOpenAccountWithBalance(UUID accountId, int balance) {
        given().baseUri(apiUrl())
                .when().get("/account/" + accountId)
                .then()
                .statusCode(200)
                .header("Content-Type", equalTo("application/json"))
                .body("accountId", equalTo(accountId.toString()))
                .body("balance", equalTo(balance))
                .body("open", equalTo(true));
    }

    private UUID newAccount() {
        return newAccount(UUID.randomUUID());
    }

    private UUID newAccount(UUID ownerId) {
        var accountId = UUID.randomUUID();
        given().baseUri(apiUrl())
                .when().post("/account/" + accountId + "?owner=" + ownerId)
                .then()
                .statusCode(201)
                .body(equalTo(""));
        return accountId;
    }
}
