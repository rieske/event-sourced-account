package lt.rieske.accounts.eventsourcing.h2;

import lt.rieske.accounts.domain.AccountEventsVisitor;
import lt.rieske.accounts.eventsourcing.AccountConsistencyTest;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventstore.Configuration;

class H2AccountConsistencyTest extends AccountConsistencyTest {

    private static final H2 H2 = new H2();

    private EventStore<AccountEventsVisitor> eventStore = Configuration.accountEventStore(H2.dataSource());

    @Override
    protected EventStore<AccountEventsVisitor> getEventStore() {
        return eventStore;
    }

}
