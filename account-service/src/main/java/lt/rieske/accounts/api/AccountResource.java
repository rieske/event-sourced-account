package lt.rieske.accounts.api;

import lt.rieske.accounts.domain.AccountEvent;
import org.slf4j.MDC;
import spark.Request;
import spark.Response;

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

    String openAccount(Request request, Response response) {
        var accountId = accountIdPathParam(request);
        var ownerId = UUID.fromString(getMandatoryQueryParameter(request, "owner"));
        MDC.put(MDC_ACCOUNT_ID_KEY, accountId.toString());

        accountService.openAccount(accountId, ownerId);

        response.header("Location", "/account/" + accountId);
        response.status(201);
        return "";
    }

    String getAccount(Request request, Response response) {
        var accountId = accountIdPathParam(request);
        MDC.put(MDC_ACCOUNT_ID_KEY, accountId.toString());

        var account = accountService.queryAccount(accountId);

        response.type(APPLICATION_JSON);
        return accountJson(account);
    }

    String deposit(Request request, Response response) {
        var accountId = accountIdPathParam(request);
        MDC.put(MDC_ACCOUNT_ID_KEY, accountId.toString());
        long amount = amountQueryParam(request);
        var transactionId = transactionIdQueryParam(request);

        accountService.deposit(accountId, amount, transactionId);

        response.status(204);
        return "";
    }

    String withdraw(Request request, Response response) {
        var accountId = accountIdPathParam(request);
        MDC.put(MDC_ACCOUNT_ID_KEY, accountId.toString());
        long amount = amountQueryParam(request);
        var transactionId = transactionIdQueryParam(request);

        accountService.withdraw(accountId, amount, transactionId);

        response.status(204);
        return "";
    }

    String transfer(Request request, Response response) {
        var sourceAccountId = accountIdPathParam(request);
        MDC.put(MDC_SOURCE_ACCOUNT_ID_KEY, sourceAccountId.toString());
        var targetAccountId = UUID.fromString(getMandatoryQueryParameter(request, "targetAccount"));
        MDC.put(MDC_TARGET_ACCOUNT_ID_KEY, targetAccountId.toString());
        long amount = amountQueryParam(request);
        var transactionId = transactionIdQueryParam(request);

        accountService.transfer(sourceAccountId, targetAccountId, amount, transactionId);

        response.status(204);
        return "";
    }

    String close(Request request, Response response) {
        var accountId = accountIdPathParam(request);

        accountService.close(accountId);

        response.status(204);
        return "";
    }

    String getEvents(Request request, Response response) {
        var accountId = accountIdPathParam(request);

        var events = accountService.getEvents(accountId);

        response.type(APPLICATION_JSON);
        return new EventStreamJsonSerializer().toJson(events);
    }

    <T extends Exception> void badRequest(T e, Request request, Response response) {
        errorJson(response, 400, e.getMessage());
    }

    <T extends Exception> void notFound(T e, Request request, Response response) {
        errorJson(response, 404, e.getMessage());
    }

    <T extends Exception> void conflict(T e, Request request, Response response) {
        response.status(409);
        response.body("");
    }

    private static UUID accountIdPathParam(Request request) {
        return UUID.fromString(request.params("accountId"));
    }

    private static long amountQueryParam(Request request) {
        return Long.parseLong(getMandatoryQueryParameter(request, "amount"));
    }

    private static UUID transactionIdQueryParam(Request request) {
        return UUID.fromString(getMandatoryQueryParameter(request, "transactionId"));
    }

    private static String getMandatoryQueryParameter(Request request, String paramName) {
        var param = request.queryParams(paramName);
        if (param == null) {
            throw new IllegalArgumentException(String.format("'%s' query parameter is required", paramName));
        }
        return param;
    }

    private static String accountJson(AccountEvent.AccountSnapshot account) {
        return "{"
                + "\"accountId\":\"" + account.accountId() + "\","
                + "\"ownerId\":\"" + account.ownerId() + "\","
                + "\"balance\":" + account.balance() + ","
                + "\"open\":" + account.open()
                + "}";
    }

    private static void errorJson(Response response, int status, String message) {
        response.status(status);
        response.type(APPLICATION_JSON);
        response.body("{\"message\":\"" + message.replaceAll("\"", "'") + "\"}");
    }

}
