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
