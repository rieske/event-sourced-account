package lt.rieske.accounts.infrastructure;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.domain.AccountSnapshotter;
import lt.rieske.accounts.eventsourcing.AggregateRepository;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.infrastructure.serialization.JsonEventSerializer;

import javax.sql.DataSource;


public class Configuration {

    public static AggregateRepository<Account> accountRepository(EventStore<Account> eventStore) {
        return new AggregateRepository<>(eventStore, Account::new);
    }

    public static AggregateRepository<Account> snapshottingAccountRepository(EventStore<Account> eventStore, int snapshottingFrequency) {
        return new AggregateRepository<>(eventStore, Account::new, new AccountSnapshotter(snapshottingFrequency));
    }

    public static EventStore<Account> accountEventStore(DataSource dataSource) {
        return new SerializingEventStore<>(new JsonEventSerializer<>(), new SqlEventStore(dataSource));
    }
}
