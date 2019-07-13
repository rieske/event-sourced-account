package lt.rieske.accounts.eventsourcing.mysql;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.eventsourcing.AccountConsistencyTest;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.infrastructure.MySql;
import lt.rieske.accounts.infrastructure.MySqlEventStore;
import lt.rieske.accounts.infrastructure.SerializingEventStore;
import lt.rieske.accounts.infrastructure.serialization.JsonEventSerializer;
import org.junit.AfterClass;

public class MySqlAccountConsistencyTest extends AccountConsistencyTest {

    private static final MySql MYSQL = new MySql();

    private EventStore<Account> eventStore = new SerializingEventStore<>(new JsonEventSerializer<>(), new MySqlEventStore(MYSQL.dataSource()));

    @AfterClass
    public static void stopDatabase() {
        MYSQL.stop();
    }

    @Override
    protected EventStore<Account> getEventStore() {
        return eventStore;
    }

    protected int depositCount() {
        return 10;
    }

    protected int threadCount() {
        return 8;
    }

}
