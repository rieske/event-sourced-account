package lt.rieske.accounts.e2e;

import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

@Tag("e2e")
@Slf4j
class ConsistencyTest {

    private static final String SERVICE_CONTAINER1 = "accounts-1_1";
    private static final String SERVICE_CONTAINER2 = "accounts-2_1";

    private static final int SERVICE_PORT = 8080;

    private static final String LB_CONTAINER = "lb_1";
    private static final int LB_PORT = 80;

    private static DockerComposeContainer environment =
            new DockerComposeContainer(new File("docker-compose.yml"))
                    .withLocalCompose(true)

                    .withLogConsumer(SERVICE_CONTAINER1, new Slf4jLogConsumer(log).withPrefix(SERVICE_CONTAINER1))
                    .withExposedService(SERVICE_CONTAINER1, SERVICE_PORT, Wait.forListeningPort())

                    .withLogConsumer(SERVICE_CONTAINER2, new Slf4jLogConsumer(log).withPrefix(SERVICE_CONTAINER2))
                    .withExposedService(SERVICE_CONTAINER2, SERVICE_PORT, Wait.forListeningPort())

                    .withExposedService(LB_CONTAINER, LB_PORT, Wait.forListeningPort());

    @BeforeAll
    static void setup() {
        environment.start();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.baseURI = lbUrl();
        RestAssured.basePath = "/api/";
    }

    @AfterAll
    static void teardown() {
        environment.stop();
    }

    private static String lbUrl() {
        return String.format("http://%s:%d",
                environment.getServiceHost(LB_CONTAINER, LB_PORT),
                environment.getServicePort(LB_CONTAINER, LB_PORT));
    }

    @Test
    void canCreateAndQueryAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        when().post("/account/" + accountId + "?owner=" + ownerId)
                .then()
                .statusCode(201)
                .header("Location", equalTo("/account/" + accountId))
                .body(equalTo(""));

        when().get("/account/" + accountId)
                .then()
                .statusCode(200)
                .header("Content-Type", equalTo("application/json"))
                .body("accountId", equalTo(accountId.toString()))
                .body("ownerId", equalTo(ownerId.toString()))
                .body("balance", equalTo(0))
                .body("open", equalTo(true));
    }

    @Test
    void accountsRemainConsistentInDistrubutedEnvironmentUnderLoad() throws InterruptedException {
        var accountId = UUID.randomUUID();
        when().post("/account/" + accountId + "?owner=" + UUID.randomUUID())
                .then()
                .statusCode(201)
                .header("Location", equalTo("/account/" + accountId))
                .body(equalTo(""));

        var operationCount = 20;
        var threadCount = 8;
        var executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < operationCount; i++) {
            var latch = new CountDownLatch(threadCount);
            var transactionId = UUID.randomUUID();
            for (int j = 0; j < threadCount; j++) {
                executor.submit(() -> {
                    withRetryOnConcurrentModification(() ->
                            when().put("/account/" + accountId + "/deposit?amount=1&transactionId=" + transactionId)
                                    .thenReturn().statusCode());
                    latch.countDown();
                });
            }
            latch.await();
        }

        when().get("/account/" + accountId)
                .then()
                .statusCode(200)
                .header("Content-Type", equalTo("application/json"))
                .body("accountId", equalTo(accountId.toString()))
                .body("balance", equalTo(operationCount));
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
}
