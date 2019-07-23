package lt.rieske.accounts.api;

import lt.rieske.accounts.domain.AccountService;
import lt.rieske.accounts.domain.AccountSnapshot;
import lt.rieske.accounts.eventsourcing.AggregateNotFoundException;
import lt.rieske.accounts.eventsourcing.h2.H2;
import lt.rieske.accounts.infrastructure.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.util.UUID;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

class AccountResourceTest {

    private static final H2 H2 = new H2();

    @BeforeAll
    static void startServer() {
        var accountResource = new AccountResource(Configuration.accountService(H2.dataSource()));

        Spark.port(8080);
        Spark.post("/account/:accountId", accountResource::createAccount);
        Spark.get("/account/:accountId", accountResource::getAccount);
        Spark.exception(IllegalArgumentException.class, accountResource::badRequest);
        Spark.exception(AggregateNotFoundException.class, accountResource::notFound);
    }

    @AfterAll
    static void stopServer() {
        Spark.stop();
    }

    @Test
    void shouldOpenAnAccount() {

        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        when().post("/account/" + accountId + "?owner=" + ownerId)
                .then()
                .statusCode(201)
                .header("Location", equalTo("/account/" + accountId))
                .body(equalTo(""));
    }

    @Test
    void shouldRequireValidUUIDForAccountId() {

        var ownerId = UUID.randomUUID();
        when().post("/account/foobar?owner=" + ownerId)
                .then()
                .statusCode(400)
                .body(equalTo("{\"message\":\"Invalid UUID string: foobar\"}"));
    }

    @Test
    void shouldRequireValidUUIDForOwnerId() {

        var accountId = UUID.randomUUID();
        when().post("/account/" + accountId + "?owner=foobar")
                .then()
                .statusCode(400)
                .body(equalTo("{\"message\":\"Invalid UUID string: foobar\"}"));
    }

    @Test
    void shouldQueryAnAccount() {

        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        var accountResource = createAccount(accountId, ownerId);

        when().get(accountResource)
                .then()
                .statusCode(200)
                .header("Content-Type", equalTo("application/json"))
                .body("accountId", equalTo(accountId.toString()))
                .body("ownerId", equalTo(ownerId.toString()))
                .body("balance", equalTo(0))
                .body("open", equalTo(true));
    }

    @Test
    void should404WhenQueryingNonExistentAccount() {

        var accountId = UUID.randomUUID();
        when().get("/account/" + accountId)
                .then()
                .statusCode(404);
    }

    private String createAccount(UUID accountId, UUID ownerId) {
        return when().post("/account/" + accountId + "?owner=" + ownerId)
                .getHeader("Location");
    }

    static class AccountResource {

        private static final String APPLICATION_JSON = "application/json";

        private final AccountService accountService;

        AccountResource(AccountService accountService) {
            this.accountService = accountService;
        }

        String createAccount(Request request, Response response) {
            var accountId = UUID.fromString(request.params("accountId"));
            var ownerId = UUID.fromString(request.queryParams("owner"));

            accountService.openAccount(accountId, ownerId);

            response.header("Location", "/account/" + accountId);
            response.status(201);
            return "";
        }

        String getAccount(Request request, Response response) {
            var accountId = UUID.fromString(request.params("accountId"));
            var account = accountService.queryAccount(accountId);

            response.type(APPLICATION_JSON);
            return accountJson(account);
        }

        <T extends Exception> void badRequest(T e, Request request, Response response) {
            errorJson(response, 400, e.getMessage());
        }

        <T extends Exception> void notFound(T e, Request request, Response response) {
            errorJson(response, 404, e.getMessage());
        }

        private String accountJson(AccountSnapshot account) {
            return "{" +
                    "\"accountId\":\"" + account.getAccountId() + "\"," +
                    "\"ownerId\":\"" + account.getOwnerId() + "\"," +
                    "\"balance\":" + account.getBalance() + "," +
                    "\"open\":" + account.isOpen() + "," +
                    "}";
        }

        private void errorJson(Response response, int status, String message) {
            response.status(status);
            response.type(APPLICATION_JSON);
            response.body("{\"message\":\"" + message + "\"}");
        }
    }
}
