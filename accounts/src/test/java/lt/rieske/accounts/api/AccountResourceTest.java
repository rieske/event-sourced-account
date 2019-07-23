package lt.rieske.accounts.api;

import io.restassured.RestAssured;
import lt.rieske.accounts.eventsourcing.h2.H2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

class AccountResourceTest {

    private static final H2 H2 = new H2();

    private static final Server SERVER = ApiConfiguration.server(H2.dataSource());

    @BeforeAll
    static void startServer() {
        RestAssured.port = SERVER.start(0);
    }

    @AfterAll
    static void stopServer() {
        SERVER.stop();
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

}
