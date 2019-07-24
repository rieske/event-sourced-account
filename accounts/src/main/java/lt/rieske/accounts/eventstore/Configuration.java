package lt.rieske.accounts.eventstore;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventstore.serialization.JsonEventSerializer;

import javax.sql.DataSource;


public class Configuration {

    public static EventStore<Account> accountEventStore(DataSource dataSource) {
        return new SerializingEventStore<>(new JsonEventSerializer<>(), new SqlEventStore(dataSource));
    }
}
