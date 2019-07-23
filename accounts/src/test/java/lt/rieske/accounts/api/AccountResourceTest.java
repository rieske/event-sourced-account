package lt.rieske.accounts.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import lt.rieske.accounts.eventsourcing.h2.H2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

class AccountResourceTest {

    private static final H2 H2 = new H2();

    private static final Server SERVER = ApiConfiguration.server(H2.dataSource());

    @BeforeAll
    static void startServer() {
        RestAssured.port = SERVER.start(0);
        RestAssured.basePath = "/api/";
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
                .body("message", equalTo("Invalid UUID string: foobar"));
    }

    @Test
    void shouldRequireValidUUIDForOwnerId() {

        var accountId = UUID.randomUUID();
        when().post("/account/" + accountId + "?owner=foobar")
                .then()
                .statusCode(400)
                .body("message", equalTo("Invalid UUID string: foobar"));
    }

    @Test
    void shouldRespondWithBadRequestOnMissingQueryParam() {

        var accountId = UUID.randomUUID();
        when().post("/account/" + accountId)
                .then()
                .statusCode(400)
                .body("message", equalTo("'owner' query parameter is required"));
    }

    @Test
    void shouldConflictOnAccountOpeningWhenAccountAlreadyExists() {

        var accountId = UUID.randomUUID();
        createAccount(accountId, UUID.randomUUID());

        when().post("/account/" + accountId + "?owner=" + UUID.randomUUID())
                .then()
                .statusCode(409);
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

    @Test
    void shouldDepositMoney() {

        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        createAccount(accountId, ownerId);

        when().put("/account/" + accountId + "/deposit?amount=" + 42 + "&transactionId=" + UUID.randomUUID())
                .then()
                .statusCode(204);

        var accountJsonPath = queryAccount(accountId);
        assertThat(accountJsonPath.getInt("balance")).isEqualTo(42);
    }

    @Test
    void depositsShouldAccumulateBalance() {

        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        createAccount(accountId, ownerId);

        when().put("/account/" + accountId + "/deposit?amount=" + 42 + "&transactionId=" + UUID.randomUUID())
                .then()
                .statusCode(204);
        when().put("/account/" + accountId + "/deposit?amount=" + 42 + "&transactionId=" + UUID.randomUUID())
                .then()
                .statusCode(204);

        var accountJsonPath = queryAccount(accountId);
        assertThat(accountJsonPath.getInt("balance")).isEqualTo(84);
    }

    @Test
    void depositTransactionsShouldBeIdempotent() {

        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        createAccount(accountId, ownerId);

        var txId = UUID.randomUUID();
        when().put("/account/" + accountId + "/deposit?amount=" + 42 + "&transactionId=" + txId)
                .then()
                .statusCode(204);
        when().put("/account/" + accountId + "/deposit?amount=" + 42 + "&transactionId=" + txId)
                .then()
                .statusCode(204);

        var accountJsonPath = queryAccount(accountId);
        assertThat(accountJsonPath.getInt("balance")).isEqualTo(42);
    }

    @Test
    void shouldNotAcceptFloatingPointDeposit() {

        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        createAccount(accountId, ownerId);

        when().put("/account/" + accountId + "/deposit?amount=42.4&transactionId=" + UUID.randomUUID())
                .then()
                .statusCode(400)
                .body("message", equalTo("For input string: '42.4'"));

        var accountJsonPath = queryAccount(accountId);
        assertThat(accountJsonPath.getInt("balance")).isZero();
    }

    @Test
    void shouldNotAcceptNonNumericDeposit() {

        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        createAccount(accountId, ownerId);

        when().put("/account/" + accountId + "/deposit?amount=banana&transactionId=" + UUID.randomUUID())
                .then()
                .statusCode(400)
                .body("message", equalTo("For input string: 'banana'"));

        var accountJsonPath = queryAccount(accountId);
        assertThat(accountJsonPath.getInt("balance")).isZero();
    }

    @Test
    void shouldNotAcceptNegativeDeposit() {

        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        createAccount(accountId, ownerId);

        when().put("/account/" + accountId + "/deposit?amount=-1&transactionId=" + UUID.randomUUID())
                .then()
                .statusCode(400)
                .body("message", equalTo("Can not deposit negative amount: -1"));

        var accountJsonPath = queryAccount(accountId);
        assertThat(accountJsonPath.getInt("balance")).isZero();
    }

    @Test
    void shouldWithdrawMoney() {

        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        createAccount(accountId, ownerId);
        deposit(accountId, 42);

        when().put("/account/" + accountId + "/withdraw?amount=" + 11)
                .then()
                .statusCode(204);

        var accountJsonPath = queryAccount(accountId);
        assertThat(accountJsonPath.getInt("balance")).isEqualTo(31);
    }

    @Test
    void shouldNotWithdrawMoneyWhenBalanceInsufficient() {

        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        createAccount(accountId, ownerId);
        deposit(accountId, 42);

        when().put("/account/" + accountId + "/withdraw?amount=" + 43)
                .then()
                .statusCode(400)
                .body("message", equalTo("Insufficient balance"));

        var accountJsonPath = queryAccount(accountId);
        assertThat(accountJsonPath.getInt("balance")).isEqualTo(42);
    }

    @Test
    void shouldNotAcceptFloatingPointWithdrawal() {

        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        createAccount(accountId, ownerId);
        deposit(accountId, 42);

        when().put("/account/" + accountId + "/withdraw?amount=42.4")
                .then()
                .statusCode(400)
                .body("message", equalTo("For input string: '42.4'"));

        var accountJsonPath = queryAccount(accountId);
        assertThat(accountJsonPath.getInt("balance")).isEqualTo(42);
    }

    @Test
    void shouldNotAcceptNonNumericWithdrawal() {

        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        createAccount(accountId, ownerId);
        deposit(accountId, 42);

        when().put("/account/" + accountId + "/withdraw?amount=banana")
                .then()
                .statusCode(400)
                .body("message", equalTo("For input string: 'banana'"));

        var accountJsonPath = queryAccount(accountId);
        assertThat(accountJsonPath.getInt("balance")).isEqualTo(42);
    }

    @Test
    void shouldNotAcceptNegativeWithdrawal() {

        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        createAccount(accountId, ownerId);
        deposit(accountId, 42);

        when().put("/account/" + accountId + "/withdraw?amount=-1")
                .then()
                .statusCode(400)
                .body("message", equalTo("Can not withdraw negative amount: -1"));

        var accountJsonPath = queryAccount(accountId);
        assertThat(accountJsonPath.getInt("balance")).isEqualTo(42);
    }

    @Test
    void shouldTransferMoneyBetweenAccounts() {

        var ownerId = UUID.randomUUID();
        var sourceAccountId = UUID.randomUUID();
        createAccount(sourceAccountId, ownerId);
        deposit(sourceAccountId, 6);

        var targetAccountId = UUID.randomUUID();
        createAccount(targetAccountId, ownerId);
        deposit(targetAccountId, 1);

        when().put("/account/" + sourceAccountId + "/transfer?targetAccount=" + targetAccountId + "&amount=" + 2)
                .then()
                .statusCode(204);

        var sourceAccountJsonPath = queryAccount(sourceAccountId);
        assertThat(sourceAccountJsonPath.getInt("balance")).isEqualTo(4);

        var targetAccountJsonPath = queryAccount(targetAccountId);
        assertThat(targetAccountJsonPath.getInt("balance")).isEqualTo(3);
    }

    @Test
    void shouldCloseAccount() {

        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        createAccount(accountId, ownerId);

        when().delete("/account/" + accountId)
                .then()
                .statusCode(204);

        var accountJsonPath = queryAccount(accountId);
        assertThat(accountJsonPath.getBoolean("open")).isFalse();
    }

    private void deposit(UUID accountId, int amount) {
        when().put("/account/" + accountId + "/deposit?amount=" + amount + "&transactionId=" + UUID.randomUUID())
                .then()
                .statusCode(204);
    }

    private JsonPath queryAccount(UUID accountId) {
        return when().get("/account/" + accountId)
                .getBody().jsonPath();
    }

    private String createAccount(UUID accountId, UUID ownerId) {
        return when().post("/account/" + accountId + "?owner=" + ownerId)
                .getHeader("Location");
    }

}
