package lt.rieske.accounts.eventsourcing.h2;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.eventsourcing.AccountConsistencyTest;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.infrastructure.SerializingEventStore;
import lt.rieske.accounts.infrastructure.SqlEventStore;
import lt.rieske.accounts.infrastructure.serialization.JsonEventSerializer;

class H2AccountConsistencyTest extends AccountConsistencyTest {

    private static final H2 H2 = new H2();

    private EventStore<Account> eventStore = new SerializingEventStore<>(new JsonEventSerializer<>(), new SqlEventStore(H2.dataSource()));

    @Override
    protected EventStore<Account> getEventStore() {
        return eventStore;
    }

}
