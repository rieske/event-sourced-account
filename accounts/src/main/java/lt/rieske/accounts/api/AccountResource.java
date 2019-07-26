package lt.rieske.accounts.api;

import lt.rieske.accounts.domain.AccountSnapshot;
import spark.Request;
import spark.Response;

import java.util.UUID;


class AccountResource {

    private static final String APPLICATION_JSON = "application/json";

    private final AccountService accountService;

    AccountResource(AccountService accountService) {
        this.accountService = accountService;
    }

    String createAccount(Request request, Response response) {
        var accountId = accountIdPathParam(request);
        var ownerId = UUID.fromString(getMandatoryQueryParameter(request, "owner"));

        accountService.openAccount(accountId, ownerId);

        response.header("Location", "/account/" + accountId);
        response.status(201);
        return "";
    }

    String getAccount(Request request, Response response) {
        var accountId = accountIdPathParam(request);

        var account = accountService.queryAccount(accountId);

        response.type(APPLICATION_JSON);
        return accountJson(account);
    }

    String deposit(Request request, Response response) {
        var accountId = accountIdPathParam(request);
        long amount = amountQueryParam(request);
        var transactionId = transactionIdQueryParam(request);

        accountService.deposit(accountId, amount, transactionId);

        response.status(204);
        return "";
    }

    String withdraw(Request request, Response response) {
        var accountId = accountIdPathParam(request);
        long amount = amountQueryParam(request);
        var transactionId = transactionIdQueryParam(request);

        accountService.withdraw(accountId, amount, transactionId);

        response.status(204);
        return "";
    }

    String transfer(Request request, Response response) {
        var sourceAccountId = accountIdPathParam(request);
        var targetAccountId = UUID.fromString(getMandatoryQueryParameter(request, "targetAccount"));
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

    public Object getEvents(Request request, Response response) {
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

    private static String accountJson(AccountSnapshot account) {
        return "{" +
                "\"accountId\":\"" + account.getAccountId() + "\"," +
                "\"ownerId\":\"" + account.getOwnerId() + "\"," +
                "\"balance\":" + account.getBalance() + "," +
                "\"open\":" + account.isOpen() + "," +
                "}";
    }

    private static void errorJson(Response response, int status, String message) {
        response.status(status);
        response.type(APPLICATION_JSON);
        response.body("{\"message\":\"" + message.replaceAll("\"", "'") + "\"}");
    }

}
