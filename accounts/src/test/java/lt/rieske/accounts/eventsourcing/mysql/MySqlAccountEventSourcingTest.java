package lt.rieske.accounts.eventsourcing.mysql;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.eventsourcing.AccountEventSourcingTest;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.infrastructure.SqlEventStore;
import lt.rieske.accounts.infrastructure.SerializingEventStore;
import lt.rieske.accounts.infrastructure.serialization.JsonEventSerializer;
import org.junit.AfterClass;

public class MySqlAccountEventSourcingTest extends AccountEventSourcingTest {

    private static final MySql MYSQL = new MySql();

    private EventStore<Account> eventStore = new SerializingEventStore<>(new JsonEventSerializer<>(), new SqlEventStore(MYSQL.dataSource()));

    @AfterClass
    public static void stopDatabase() {
        MYSQL.stop();
    }

    @Override
    protected EventStore<Account> getEventStore() {
        return eventStore;
    }

}
