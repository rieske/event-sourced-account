package lt.rieske.accounts.eventstore;

import lt.rieske.accounts.domain.AccountEventsVisitor;
import lt.rieske.accounts.eventsourcing.EventStore;

import javax.sql.DataSource;


public class Configuration {

    public static BlobEventStore mysqlAccountEventStore(DataSource dataSource) {
        return new MySqlEventStore(dataSource);
    }

    public static BlobEventStore postgresAccountEventStore(DataSource dataSource) {
        return new PosrgresEventStore(dataSource);
    }

    public static EventStore<AccountEventsVisitor> accountEventStore(BlobEventStore blobEventStore) {
        return new SerializingEventStore<>(new MessagePackAccountEventSerializer(), blobEventStore);
    }
}
