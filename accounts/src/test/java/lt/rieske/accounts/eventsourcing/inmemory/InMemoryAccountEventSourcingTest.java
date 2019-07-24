package lt.rieske.accounts.eventsourcing.inmemory;

import lt.rieske.accounts.domain.AccountEventsVisitor;
import lt.rieske.accounts.eventsourcing.AccountEventSourcingTest;
import lt.rieske.accounts.eventsourcing.EventStore;

class InMemoryAccountEventSourcingTest extends AccountEventSourcingTest {

    private final InMemoryEventStore<AccountEventsVisitor> eventStore = new InMemoryEventStore<>();

    @Override
    protected EventStore<AccountEventsVisitor> getEventStore() {
        return eventStore;
    }

}
