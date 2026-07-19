package lt.rieske.accounts.api;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.PathTemplateMatch;
import lt.rieske.accounts.domain.AccountEvent;
import org.slf4j.MDC;

import java.util.Deque;
import java.util.UUID;


class AccountResource {

    private static final String APPLICATION_JSON = "application/json";

    private static final String MDC_ACCOUNT_ID_KEY = "accountId";
    private static final String MDC_SOURCE_ACCOUNT_ID_KEY = "sourceAccountId";
    private static final String MDC_TARGET_ACCOUNT_ID_KEY = "targetAccountId";

    private final AccountService accountService;

    AccountResource(AccountService accountService) {
        this.accountService = accountService;
    }

    void openAccount(HttpServerExchange exchange) {
        var accountId = accountIdPathParam(exchange);
        var ownerId = UUID.fromString(getMandatoryQueryParameter(exchange, "owner"));
        MDC.put(MDC_ACCOUNT_ID_KEY, accountId.toString());

        accountService.openAccount(accountId, ownerId);

        exchange.setStatusCode(201);
        exchange.getResponseHeaders().put(new HttpString("Location"), "/account/" + accountId);
    }

    void getAccount(HttpServerExchange exchange) {
        var accountId = accountIdPathParam(exchange);
        MDC.put(MDC_ACCOUNT_ID_KEY, accountId.toString());

        var account = accountService.queryAccount(accountId);

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, APPLICATION_JSON);
        exchange.getResponseSender().send(accountJson(account));
    }

    void deposit(HttpServerExchange exchange) {
        var accountId = accountIdPathParam(exchange);
        MDC.put(MDC_ACCOUNT_ID_KEY, accountId.toString());
        long amount = amountQueryParam(exchange);
        var transactionId = transactionIdQueryParam(exchange);

        accountService.deposit(accountId, amount, transactionId);

        exchange.setStatusCode(204);
    }

    void withdraw(HttpServerExchange exchange) {
        var accountId = accountIdPathParam(exchange);
        MDC.put(MDC_ACCOUNT_ID_KEY, accountId.toString());
        long amount = amountQueryParam(exchange);
        var transactionId = transactionIdQueryParam(exchange);

        accountService.withdraw(accountId, amount, transactionId);

        exchange.setStatusCode(204);
    }

    void transfer(HttpServerExchange exchange) {
        var sourceAccountId = accountIdPathParam(exchange);
        MDC.put(MDC_SOURCE_ACCOUNT_ID_KEY, sourceAccountId.toString());
        var targetAccountId = UUID.fromString(getMandatoryQueryParameter(exchange, "targetAccount"));
        MDC.put(MDC_TARGET_ACCOUNT_ID_KEY, targetAccountId.toString());
        long amount = amountQueryParam(exchange);
        var transactionId = transactionIdQueryParam(exchange);

        accountService.transfer(sourceAccountId, targetAccountId, amount, transactionId);

        exchange.setStatusCode(204);
    }

    void close(HttpServerExchange exchange) {
        var accountId = accountIdPathParam(exchange);

        accountService.close(accountId);

        exchange.setStatusCode(204);
    }

    void getEvents(HttpServerExchange exchange) {
        var accountId = accountIdPathParam(exchange);

        var events = accountService.getEvents(accountId);

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, APPLICATION_JSON);
        exchange.getResponseSender().send(new EventStreamJsonSerializer().toJson(events));
    }

    void badRequest(Exception e, HttpServerExchange exchange) {
        errorJson(exchange, 400, e.getMessage());
    }

    void notFound(Exception e, HttpServerExchange exchange) {
        errorJson(exchange, 404, e.getMessage());
    }

    void conflict(Exception e, HttpServerExchange exchange) {
        exchange.setStatusCode(409);
    }

    private static UUID accountIdPathParam(HttpServerExchange exchange) {
        return UUID.fromString(pathParam(exchange, "accountId"));
    }

    private static String pathParam(HttpServerExchange exchange, String name) {
        PathTemplateMatch match = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
        if (match == null) {
            throw new IllegalArgumentException("Missing path parameter: " + name);
        }
        String value = match.getParameters().get(name);
        if (value == null) {
            throw new IllegalArgumentException("Missing path parameter: " + name);
        }
        return value;
    }

    private static long amountQueryParam(HttpServerExchange exchange) {
        return Long.parseLong(getMandatoryQueryParameter(exchange, "amount"));
    }

    private static UUID transactionIdQueryParam(HttpServerExchange exchange) {
        return UUID.fromString(getMandatoryQueryParameter(exchange, "transactionId"));
    }

    private static String getMandatoryQueryParameter(HttpServerExchange exchange, String paramName) {
        Deque<String> values = exchange.getQueryParameters().get(paramName);
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(String.format("'%s' query parameter is required", paramName));
        }
        return values.getFirst();
    }

    private static String accountJson(AccountEvent.AccountSnapshot account) {
        return "{"
                + "\"accountId\":\"" + account.accountId() + "\","
                + "\"ownerId\":\"" + account.ownerId() + "\","
                + "\"balance\":" + account.balance() + ","
                + "\"open\":" + account.open()
                + "}";
    }

    private static void errorJson(HttpServerExchange exchange, int status, String message) {
        exchange.setStatusCode(status);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, APPLICATION_JSON);
        exchange.getResponseSender().send("{\"message\":\"" + message.replace("\"", "'") + "\"}");
    }
}
