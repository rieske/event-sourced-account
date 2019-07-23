package lt.rieske.accounts.eventsourcing.mysql;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.eventsourcing.AccountConsistencyTest;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.infrastructure.SerializingEventStore;
import lt.rieske.accounts.infrastructure.SqlEventStore;
import lt.rieske.accounts.infrastructure.serialization.JsonEventSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;

@Tag("integration")
class MySqlAccountConsistencyTest extends AccountConsistencyTest {

    private static final MySql MYSQL = new MySql();

    private EventStore<Account> eventStore = new SerializingEventStore<>(new JsonEventSerializer<>(), new SqlEventStore(MYSQL.dataSource()));

    @AfterAll
    static void stopDatabase() {
        MYSQL.stop();
    }

    @Override
    protected EventStore<Account> getEventStore() {
        return eventStore;
    }

    protected int operationCount() {
        return 10;
    }

    protected int threadCount() {
        return 8;
    }

}
