package lt.rieske.accounts.api;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.domain.AccountEventsVisitor;
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

    public static AggregateRepository<Account, AccountEventsVisitor> accountRepository(EventStore<AccountEventsVisitor> eventStore) {
        return new AggregateRepository<>(eventStore, Account::new);
    }

    public static AggregateRepository<Account, AccountEventsVisitor> snapshottingAccountRepository(EventStore<AccountEventsVisitor> eventStore, int snapshottingFrequency) {
        return new AggregateRepository<>(eventStore, Account::new, new AccountSnapshotter(snapshottingFrequency));
    }

}
