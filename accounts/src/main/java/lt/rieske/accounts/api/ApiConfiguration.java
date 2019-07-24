package lt.rieske.accounts.api;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.domain.AccountSnapshotter;
import lt.rieske.accounts.eventsourcing.AggregateRepository;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventstore.Configuration;

import javax.sql.DataSource;

public class ApiConfiguration {

    public static Server server(DataSource dataSource) {
        var eventStore = Configuration.accountEventStore(dataSource);
        var accountRepository = snapshottingAccountRepository(eventStore, 50);
        var accountService = new AccountService(accountRepository, eventStore);
        var accountResource = new AccountResource(accountService);
        return new Server(accountResource);
    }

    public static AggregateRepository<Account> accountRepository(EventStore<Account> eventStore) {
        return new AggregateRepository<>(eventStore, Account::new);
    }

    public static AggregateRepository<Account> snapshottingAccountRepository(EventStore<Account> eventStore, int snapshottingFrequency) {
        return new AggregateRepository<>(eventStore, Account::new, new AccountSnapshotter(snapshottingFrequency));
    }

}
