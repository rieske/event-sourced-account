package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.domain.AccountEventsVisitor;
import lt.rieske.accounts.eventstore.Configuration;
import lt.rieske.accounts.eventstore.SqlEventStoreTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;

import javax.sql.DataSource;

public abstract class SqlEventStoreIntegrationTests {

    private final EventStore<AccountEventsVisitor> eventStore = Configuration.accountEventStore(dataSource());

    protected abstract DataSource dataSource();

    @Nested
    class SpecificSqlEventStoreTest extends SqlEventStoreTest {

        protected DataSource dataSource() {
            return SqlEventStoreIntegrationTests.this.dataSource();
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
