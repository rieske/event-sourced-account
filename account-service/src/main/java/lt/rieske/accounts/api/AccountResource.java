package lt.rieske.accounts.api;

import io.helidon.common.http.MediaType;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import lt.rieske.accounts.domain.AccountSnapshot;

import java.util.UUID;


class AccountResource {

    private final AccountService accountService;

    AccountResource(AccountService accountService) {
        this.accountService = accountService;
    }

    void openAccount(ServerRequest request, ServerResponse response) {
        var accountId = accountIdPathParam(request);
        var ownerId = UUID.fromString(getMandatoryQueryParameter(request, "owner"));

        accountService.openAccount(accountId, ownerId);

        response.addHeader("Location", "/account/" + accountId);
        response.status(201);
        response.send();
    }

    void getAccount(ServerRequest request, ServerResponse response) {
        var accountId = accountIdPathParam(request);

        var account = accountService.queryAccount(accountId);

        response.headers().contentType(MediaType.APPLICATION_JSON);
        response.send(accountJson(account));
    }

    void deposit(ServerRequest request, ServerResponse response) {
        var accountId = accountIdPathParam(request);
        long amount = amountQueryParam(request);
        var transactionId = transactionIdQueryParam(request);

        accountService.deposit(accountId, amount, transactionId);

        response.status(204);
        response.send();
    }

    void withdraw(ServerRequest request, ServerResponse response) {
        var accountId = accountIdPathParam(request);
        long amount = amountQueryParam(request);
        var transactionId = transactionIdQueryParam(request);

        accountService.withdraw(accountId, amount, transactionId);

        response.status(204);
        response.send();
    }

    void transfer(ServerRequest request, ServerResponse response) {
        var sourceAccountId = accountIdPathParam(request);
        var targetAccountId = UUID.fromString(getMandatoryQueryParameter(request, "targetAccount"));
        long amount = amountQueryParam(request);
        var transactionId = transactionIdQueryParam(request);

        accountService.transfer(sourceAccountId, targetAccountId, amount, transactionId);

        response.status(204);
        response.send();
    }

    void close(ServerRequest request, ServerResponse response) {
        var accountId = accountIdPathParam(request);

        accountService.close(accountId);

        response.status(204);
        response.send();
    }

    void getEvents(ServerRequest request, ServerResponse response) {
        var accountId = accountIdPathParam(request);

        var events = accountService.getEvents(accountId);

        response.headers().contentType(MediaType.APPLICATION_JSON);
        response.send(new EventStreamJsonSerializer().toJson(events));
    }

    <T extends Exception> void badRequest(ServerRequest request, ServerResponse response, T e) {
        errorJson(response, 400, e.getMessage());
    }

    <T extends Exception> void notFound(ServerRequest request, ServerResponse response, T e) {
        errorJson(response, 404, e.getMessage());
    }

    <T extends Exception> void conflict(ServerRequest request, ServerResponse response, T e) {
        response.status(409);
        response.send();
    }

    private static UUID accountIdPathParam(ServerRequest request) {
        return UUID.fromString(request.path().param("accountId"));
    }

    private static long amountQueryParam(ServerRequest request) {
        return Long.parseLong(getMandatoryQueryParameter(request, "amount"));
    }

    private static UUID transactionIdQueryParam(ServerRequest request) {
        return UUID.fromString(getMandatoryQueryParameter(request, "transactionId"));
    }

    private static String getMandatoryQueryParameter(ServerRequest request, String paramName) {
        var param = request.queryParams().first(paramName);
        if (param.isEmpty()) {
            throw new IllegalArgumentException(String.format("'%s' query parameter is required", paramName));
        }
        return param.get();
    }

    private static String accountJson(AccountSnapshot account) {
        return "{" +
                "\"accountId\":\"" + account.accountId() + "\"," +
                "\"ownerId\":\"" + account.ownerId() + "\"," +
                "\"balance\":" + account.balance() + "," +
                "\"open\":" + account.open() +
                "}";
    }

    private static void errorJson(ServerResponse response, int status, String message) {
        response.status(status);
        response.headers().contentType(MediaType.APPLICATION_JSON);
        response.send("{\"message\":\"" + message.replaceAll("\"", "'") + "\"}");
    }

}
