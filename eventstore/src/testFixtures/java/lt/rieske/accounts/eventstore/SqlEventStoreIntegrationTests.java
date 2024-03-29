package lt.rieske.accounts.eventstore;

import lt.rieske.accounts.domain.AccountEvent;
import lt.rieske.accounts.eventsourcing.AccountConsistencyTest;
import lt.rieske.accounts.eventsourcing.AccountEventSourcingTest;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventsourcing.IdempotencyTest;
import lt.rieske.accounts.eventsourcing.MoneyTransferTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public abstract class SqlEventStoreIntegrationTests {

    protected abstract DataSource dataSource();

    protected abstract BlobEventStore blobEventStore();

    protected abstract void setUUID(PreparedStatement statement, int column, UUID uuid) throws SQLException;


    private BlobEventStore blobEventStore;
    private EventStore<AccountEvent> eventStore;

    @BeforeEach
    void createEventStore() {
        this.blobEventStore = blobEventStore();
        this.eventStore = Configuration.accountEventStore(blobEventStore);
    }

    @Nested
    class SpecificSqlEventStoreTest extends SqlEventStoreTest {

        protected DataSource dataSource() {
            return SqlEventStoreIntegrationTests.this.dataSource();
        }

        @Override
        protected BlobEventStore blobEventStore() {
            return blobEventStore;
        }

        @Override
        protected void setUUID(PreparedStatement statement, int column, UUID uuid) throws SQLException {
            SqlEventStoreIntegrationTests.this.setUUID(statement, column, uuid);
        }
    }

    @Nested
    class SqlAccountEventSourcingTest extends AccountEventSourcingTest {

        @Override
        protected EventStore<AccountEvent> getEventStore() {
            return eventStore;
        }
    }

    @Nested
    class SqlMoneyTransferTest extends MoneyTransferTest {

        @Override
        protected EventStore<AccountEvent> getEventStore() {
            return eventStore;
        }

    }

    @Nested
    class SqlIdempotencyTest extends IdempotencyTest {

        @Override
        protected EventStore<AccountEvent> getEventStore() {
            return eventStore;
        }
    }

    @Nested
    class SqlAccountConsistencyTest extends AccountConsistencyTest {

        @Override
        protected EventStore<AccountEvent> getEventStore() {
            return eventStore;
        }

        @Override
        protected int operationCount() {
            return 10;
        }

        @Override
        protected int threadCount() {
            return 8;
        }
    }
}
