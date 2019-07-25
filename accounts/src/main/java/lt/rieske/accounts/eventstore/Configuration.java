package lt.rieske.accounts.eventstore;

import lt.rieske.accounts.domain.AccountEventsVisitor;
import lt.rieske.accounts.eventsourcing.EventStore;

import javax.sql.DataSource;


public class Configuration {

    public static EventStore<AccountEventsVisitor> accountEventStore(DataSource dataSource) {
        return new SerializingEventStore<>(new JsonEventSerializer<>(), new SqlEventStore(dataSource));
    }
}
