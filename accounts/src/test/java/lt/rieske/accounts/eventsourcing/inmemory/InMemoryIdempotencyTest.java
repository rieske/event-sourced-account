package lt.rieske.accounts.eventsourcing.inmemory;

import lt.rieske.accounts.domain.AccountEventsVisitor;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventsourcing.IdempotencyTest;

class InMemoryIdempotencyTest extends IdempotencyTest {

    private final InMemoryEventStore<AccountEventsVisitor> eventStore = new InMemoryEventStore<>();

    @Override
    protected EventStore<AccountEventsVisitor> getEventStore() {
        return eventStore;
    }

}
