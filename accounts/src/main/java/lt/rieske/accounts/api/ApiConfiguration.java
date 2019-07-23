package lt.rieske.accounts.api;

import lt.rieske.accounts.infrastructure.Configuration;

import javax.sql.DataSource;

public class ApiConfiguration {

    public static Server server(DataSource dataSource) {
        var eventStore = Configuration.accountEventStore(dataSource);
        var accountRepository = Configuration.snapshottingAccountRepository(eventStore, 50);
        var accountService = new AccountService(accountRepository);
        var accountResource = new AccountResource(accountService);
        return new Server(accountResource);
    }
}
