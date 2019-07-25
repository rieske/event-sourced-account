package lt.rieske.accounts.api;

import io.restassured.path.json.JsonPath;
import lt.rieske.accounts.eventsourcing.h2.H2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class AccountResourceTest {

    private static final H2 H2 = new H2();

    private static final Server SERVER = ApiConfiguration.server(H2.dataSource());
    private static int serverPort;

    @BeforeAll
    static void startServer() {
        serverPort = SERVER.start(0);
    }

    @AfterAll
    static void tearDown() {
        SERVER.stop();
    }

    private static String baseUri() {
        return String.format("http://localhost:%d/api", serverPort);
    }

    @Test
    void shouldOpenAnAccount() {

        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        given().baseUri(baseUri())
                .when().post("/account/" + accountId + "?owner=" + ownerId)
                .then()
                .statusCode(201)
                .header("Location", equalTo("/account/" + accountId))
                .body(equalTo(""));
    }

    @Test
    void shouldRequireValidUUIDForAccountId() {

        var ownerId = UUID.randomUUID();
        given().baseUri(baseUri())
                .when().post("/account/foobar?owner=" + ownerId)
                .then()
                .statusCode(400)
                .body("message", equalTo("Invalid UUID string: foobar"));
    }

    @Test
    void shouldRequireValidUUIDForOwnerId() {

        var accountId = UUID.randomUUID();
        given().baseUri(baseUri())
                .when().post("/account/" + accountId + "?owner=foobar")
                .then()
                .statusCode(400)
                .body("message", equalTo("Invalid UUID string: foobar"));
    }

    @Test
    void shouldRespondWithBadRequestOnMissingQueryParam() {

        var accountId = UUID.randomUUID();
        given().baseUri(baseUri())
                .when().post("/account/" + accountId)
                .then()
                .statusCode(400)
                .body("message", equalTo("'owner' query parameter is required"));
    }

    @Test
    void shouldConflictOnAccountOpeningWhenAccountAlreadyExists() {

        var accountId = UUID.randomUUID();
        createAccount(accountId, UUID.randomUUID());

        given().baseUri(baseUri())
                .when().post("/account/" + accountId + "?owner=" + UUID.randomUUID())
                .then()
                .statusCode(409);
    }

    @Test
    void shouldQueryAnAccount() {

        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        var accountResource = createAccount(accountId, ownerId);

        given().baseUri(baseUri())
                .when().get(accountResource)
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
        given().baseUri(baseUri())
                .when().get("/account/" + accountId)
                .then()
                .statusCode(404);
    }

    @Test
    void shouldDepositMoney() {

        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        createAccount(accountId, ownerId);

        given().baseUri(baseUri())
                .when().put("/account/" + accountId + "/deposit?amount=" + 42 + "&transactionId=" + UUID.randomUUID())
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

        given().baseUri(baseUri())
                .when().put("/account/" + accountId + "/deposit?amount=" + 42 + "&transactionId=" + UUID.randomUUID())
                .then()
                .statusCode(204);
        given().baseUri(baseUri())
                .when().put("/account/" + accountId + "/deposit?amount=" + 42 + "&transactionId=" + UUID.randomUUID())
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
        given().baseUri(baseUri())
                .when().put("/account/" + accountId + "/deposit?amount=" + 42 + "&transactionId=" + txId)
                .then()
                .statusCode(204);
        given().baseUri(baseUri())
                .when().put("/account/" + accountId + "/deposit?amount=" + 42 + "&transactionId=" + txId)
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

        given().baseUri(baseUri())
                .when().put("/account/" + accountId + "/deposit?amount=42.4&transactionId=" + UUID.randomUUID())
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

        given().baseUri(baseUri())
                .when().put("/account/" + accountId + "/deposit?amount=banana&transactionId=" + UUID.randomUUID())
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

        given().baseUri(baseUri())
                .when().put("/account/" + accountId + "/deposit?amount=-1&transactionId=" + UUID.randomUUID())
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

        given().baseUri(baseUri())
                .when().put("/account/" + accountId + "/withdraw?amount=" + 11 + "&transactionId=" + UUID.randomUUID())
                .then()
                .statusCode(204);

        var accountJsonPath = queryAccount(accountId);
        assertThat(accountJsonPath.getInt("balance")).isEqualTo(31);
    }

    @Test
    void withdrawalTransactionShouldBeIdempotent() {

        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        createAccount(accountId, ownerId);
        deposit(accountId, 42);

        var txId = UUID.randomUUID();
        given().baseUri(baseUri())
                .when().put("/account/" + accountId + "/withdraw?amount=" + 30 + "&transactionId=" + txId)
                .then()
                .statusCode(204);
        given().baseUri(baseUri())
                .when().put("/account/" + accountId + "/withdraw?amount=" + 30 + "&transactionId=" + txId)
                .then()
                .statusCode(204);

        var accountJsonPath = queryAccount(accountId);
        assertThat(accountJsonPath.getInt("balance")).isEqualTo(12);
    }

    @Test
    void shouldNotWithdrawMoneyWhenBalanceInsufficient() {

        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        createAccount(accountId, ownerId);
        deposit(accountId, 42);

        given().baseUri(baseUri())
                .when().put("/account/" + accountId + "/withdraw?amount=" + 43 + "&transactionId=" + UUID.randomUUID())
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

        given().baseUri(baseUri())
                .when().put("/account/" + accountId + "/withdraw?amount=42.4")
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

        given().baseUri(baseUri())
                .when().put("/account/" + accountId + "/withdraw?amount=banana")
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

        given().baseUri(baseUri())
                .when().put("/account/" + accountId + "/withdraw?amount=-1&transactionId=" + UUID.randomUUID())
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

        given().baseUri(baseUri())
                .when().put("/account/" + sourceAccountId + "/transfer?targetAccount="
                + targetAccountId + "&amount=2&transactionId=" + UUID.randomUUID())
                .then()
                .statusCode(204);

        var sourceAccountJsonPath = queryAccount(sourceAccountId);
        assertThat(sourceAccountJsonPath.getInt("balance")).isEqualTo(4);

        var targetAccountJsonPath = queryAccount(targetAccountId);
        assertThat(targetAccountJsonPath.getInt("balance")).isEqualTo(3);
    }

    @Test
    void moneyTransferTransactionsShouldBeIdempotent() {

        var ownerId = UUID.randomUUID();
        var sourceAccountId = UUID.randomUUID();
        createAccount(sourceAccountId, ownerId);
        deposit(sourceAccountId, 100);

        var targetAccountId = UUID.randomUUID();
        createAccount(targetAccountId, ownerId);

        var txId = UUID.randomUUID();
        given().baseUri(baseUri())
                .when().put("/account/" + sourceAccountId + "/transfer?targetAccount=" + targetAccountId + "&amount=60&transactionId=" + txId)
                .then()
                .statusCode(204);
        given().baseUri(baseUri())
                .when().put("/account/" + sourceAccountId + "/transfer?targetAccount=" + targetAccountId + "&amount=60&transactionId=" + txId)
                .then()
                .statusCode(204);

        var sourceAccountJsonPath = queryAccount(sourceAccountId);
        assertThat(sourceAccountJsonPath.getInt("balance")).isEqualTo(40);

        var targetAccountJsonPath = queryAccount(targetAccountId);
        assertThat(targetAccountJsonPath.getInt("balance")).isEqualTo(60);
    }

    @Test
    void should404WhenTransferringToNonExistentAccount() {

        var ownerId = UUID.randomUUID();
        var sourceAccountId = UUID.randomUUID();
        createAccount(sourceAccountId, ownerId);
        deposit(sourceAccountId, 6);

        var targetAccountId = UUID.randomUUID();

        given().baseUri(baseUri())
                .when().put("/account/" + sourceAccountId + "/transfer?targetAccount="
                + targetAccountId + "&amount=2&transactionId=" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("message", equalTo("Aggregate not found, aggregateId: " + targetAccountId));

        var sourceAccountJsonPath = queryAccount(sourceAccountId);
        assertThat(sourceAccountJsonPath.getInt("balance")).isEqualTo(6);
    }

    @Test
    void shouldCloseAccount() {

        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        createAccount(accountId, ownerId);

        given().baseUri(baseUri())
                .when().delete("/account/" + accountId)
                .then()
                .statusCode(204);

        var accountJsonPath = queryAccount(accountId);
        assertThat(accountJsonPath.getBoolean("open")).isFalse();
    }

    @Test
    void should404WhenClosingNonExistentAccount() {
        given().baseUri(baseUri())
                .when().delete("/account/" + UUID.randomUUID())
                .then()
                .statusCode(404);
    }

    @Test
    void shouldQueryAccountEvents() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        createAccount(accountId, ownerId);
        deposit(accountId, 5);
        deposit(accountId, 12);

        given().baseUri(baseUri())
                .when().get("/account/" + accountId + "/events")
                .then()
                .statusCode(200)
                .header("Content-Type", equalTo("application/json"))
                .body("size()", equalTo(3))
                .body("[0].sequenceNumber", equalTo(1))
                .body("[0].transactionId", notNullValue())
                .body("[0].type", equalTo("AccountOpenedEvent"))
                .body("[0].ownerId", equalTo(ownerId.toString()))

                .body("[1].sequenceNumber", equalTo(2))
                .body("[1].transactionId", notNullValue())
                .body("[1].type", equalTo("MoneyDepositedEvent"))
                .body("[1].amountDeposited", equalTo(5))
                .body("[1].balance", equalTo(5))

                .body("[2].sequenceNumber", equalTo(3))
                .body("[2].transactionId", notNullValue())
                .body("[2].type", equalTo("MoneyDepositedEvent"))
                .body("[2].amountDeposited", equalTo(12))
                .body("[2].balance", equalTo(17));
    }

    @Test
    void nonExistingAccountShouldNotHaveEvents() {
        var accountId = UUID.randomUUID();

        given().baseUri(baseUri())
                .when().get("/account/" + accountId + "/events")
                .then()
                .statusCode(200)
                .header("Content-Type", equalTo("application/json"))
                .body(equalTo("[]"));
    }

    private void deposit(UUID accountId, int amount) {
        given().baseUri(baseUri())
                .when().put("/account/" + accountId + "/deposit?amount=" + amount + "&transactionId=" + UUID.randomUUID())
                .then()
                .statusCode(204);
    }

    private JsonPath queryAccount(UUID accountId) {
        return given().baseUri(baseUri())
                .when().get("/account/" + accountId)
                .getBody().jsonPath();
    }

    private String createAccount(UUID accountId, UUID ownerId) {
        return given().baseUri(baseUri())
                .when().post("/account/" + accountId + "?owner=" + ownerId)
                .getHeader("Location");
    }

}
