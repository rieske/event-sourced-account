package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.domain.AccountEventsVisitor;
import lt.rieske.accounts.eventstore.BlobEventStore;
import lt.rieske.accounts.eventstore.Configuration;
import lt.rieske.accounts.eventstore.SqlEventStoreTest;
import org.junit.jupiter.api.Nested;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

abstract class SqlEventStoreIntegrationTests {

    protected abstract DataSource dataSource();

    protected abstract BlobEventStore blobEventStore();

    protected abstract void setUUID(PreparedStatement statement, int column, UUID uuid) throws SQLException;

    private final EventStore<AccountEventsVisitor> eventStore = Configuration.accountEventStore(blobEventStore());

    @Nested
    class SpecificSqlEventStoreTest extends SqlEventStoreTest {

        protected DataSource dataSource() {
            return SqlEventStoreIntegrationTests.this.dataSource();
        }

        @Override
        protected BlobEventStore blobEventStore() {
            return SqlEventStoreIntegrationTests.this.blobEventStore();
        }

        @Override
        protected void setUUID(PreparedStatement statement, int column, UUID uuid) throws SQLException {
            SqlEventStoreIntegrationTests.this.setUUID(statement, column, uuid);
        }
    }

    @Nested
    class SqlAccountEventSourcingTest extends AccountEventSourcingTest {

        @Override
        protected EventStore<AccountEventsVisitor> getEventStore() {
            return eventStore;
        }
    }

    @Nested
    class SqlMoneyTransferTest extends MoneyTransferTest {

        @Override
        protected EventStore<AccountEventsVisitor> getEventStore() {
            return eventStore;
        }

    }

    @Nested
    class SqlIdempotencyTest extends IdempotencyTest {

        @Override
        protected EventStore<AccountEventsVisitor> getEventStore() {
            return eventStore;
        }
    }

    @Nested
    class SqlAccountConsistencyTest extends AccountConsistencyTest {

        @Override
        protected EventStore<AccountEventsVisitor> getEventStore() {
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
