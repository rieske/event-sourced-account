package lt.rieske.accounts.e2e;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.assertThat;

@Tag("e2e")
@Slf4j
class ConsistencyTest {

    private static final String SERVICE_CONTAINER = "account";

    private static final int SERVICE_PORT = 8080;

    private static final String LB_CONTAINER = "lb_1";
    private static final int LB_PORT = 10000;

    private static DockerComposeContainer environment =
            new DockerComposeContainer(new File("e2e-test.yml"))
                    .withLocalCompose(true)

                    .withLogConsumer(SERVICE_CONTAINER, new Slf4jLogConsumer(log).withPrefix(SERVICE_CONTAINER))

                    .withExposedService(SERVICE_CONTAINER, 1, SERVICE_PORT, Wait.forListeningPort())
                    .withExposedService(SERVICE_CONTAINER, 1, SERVICE_PORT, Wait.forHttp("/ping").forStatusCode(200))
                    .withExposedService(SERVICE_CONTAINER, 2, SERVICE_PORT, Wait.forListeningPort())
                    .withExposedService(SERVICE_CONTAINER, 2, SERVICE_PORT, Wait.forHttp("/ping").forStatusCode(200))

                    .withLogConsumer(LB_CONTAINER, new Slf4jLogConsumer(log).withPrefix(LB_CONTAINER))

                    .withExposedService(LB_CONTAINER, LB_PORT, Wait.forListeningPort())
                    .withExposedService(LB_CONTAINER, LB_PORT, Wait.forHttp("/ping").forStatusCode(200));

    @BeforeAll
    static void setup() {
        environment.start();
    }

    @AfterAll
    static void teardown() {
        environment.stop();
    }

    private AccountClient client;

    @BeforeEach
    void createClient() {
        this.client = new AccountClient(apiUrl());
    }

    private static String apiUrl() {
        return String.format("http://%s:%d/api",
                environment.getServiceHost(LB_CONTAINER, LB_PORT),
                environment.getServicePort(LB_CONTAINER, LB_PORT));
    }

    @Test
    void canCreateAndQueryAccount() {
        var ownerId = UUID.randomUUID();
        var accountId = newAccount(ownerId);

        var accountJson = client.queryAccount(accountId);
        assertThat(accountJson).isEqualTo("{" +
                "\"accountId\":\"" + accountId + "\"," +
                "\"ownerId\":\"" + ownerId + "\"," +
                "\"balance\":0," +
                "\"open\":true" +
                "}");
    }

    @Test
    void accountsRemainConsistentInDistributedEnvironmentUnderLoad() throws InterruptedException {
        var accountId = newAccount();

        var operationCount = 1000;
        var threadCount = 8;
        var executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < operationCount; i++) {
            var latch = new CountDownLatch(threadCount);
            var transactionId = UUID.randomUUID();
            for (int j = 0; j < threadCount; j++) {
                executor.submit(() -> {
                    withRetryOnConflict(() -> client.deposit(accountId, 1, transactionId));
                    latch.countDown();
                });
            }
            latch.await();
        }
        executor.shutdown();

        assertOpenAccountWithBalance(accountId, operationCount);
    }

    private void assertOpenAccountWithBalance(UUID accountId, int balance) {
        var accountJson = client.queryAccount(accountId);
        assertThat(accountJson).contains("\"accountId\":\"" + accountId + "\"");
        assertThat(accountJson).contains("\"balance\":" + balance);
        assertThat(accountJson).contains("\"open\":true");
    }

    private UUID newAccount() {
        return newAccount(UUID.randomUUID());
    }

    private UUID newAccount(UUID ownerId) {
        var accountId = UUID.randomUUID();
        client.openAccount(accountId, ownerId);
        return accountId;
    }

    private static void withRetryOnConflict(Supplier<Integer> s) {
        concurrentModificationRetryLoop:
        while (true) {
            int response = s.get();
            switch (response) {
                case 204:
                    break concurrentModificationRetryLoop;
                case 409:
                    continue;
                default:
                    throw new IllegalStateException("Unexpected response from server: " + response);
            }
        }
    }
}
