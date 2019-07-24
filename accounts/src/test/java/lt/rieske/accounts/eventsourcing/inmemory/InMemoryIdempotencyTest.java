package lt.rieske.accounts.eventsourcing.inmemory;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventsourcing.IdempotencyTest;

class InMemoryIdempotencyTest extends IdempotencyTest {

    private final InMemoryEventStore<Account> eventStore = new InMemoryEventStore<>();

    @Override
    protected EventStore<Account> getEventStore() {
        return eventStore;
    }

}
