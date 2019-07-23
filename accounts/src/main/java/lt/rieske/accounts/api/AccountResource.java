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

    String deposit(Request request, Response response) {
        var accountId = UUID.fromString(request.params("accountId"));
        int amount = Integer.parseInt(request.queryParams("amount"));

        accountService.deposit(accountId, amount);

        response.status(204);
        return "";
    }

    String withdraw(Request request, Response response) {
        var accountId = UUID.fromString(request.params("accountId"));
        int amount = Integer.parseInt(request.queryParams("amount"));

        accountService.withdraw(accountId, amount);

        response.status(204);
        return "";
    }

    String transfer(Request request, Response response) {
        var sourceAccountId = UUID.fromString(request.params("accountId"));
        var targetAccountId = UUID.fromString(request.queryParams("targetAccount"));
        int amount = Integer.parseInt(request.queryParams("amount"));

        accountService.transfer(sourceAccountId, targetAccountId, amount);

        response.status(204);
        return "";
    }

    String close(Request request, Response response) {
        var accountId = UUID.fromString(request.params("accountId"));

        accountService.close(accountId);

        response.status(204);
        return "";
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
        response.body("{\"message\":\"" + message.replaceAll("\"", "'") + "\"}");
    }
}
