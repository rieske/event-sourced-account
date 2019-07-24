package lt.rieske.accounts.eventsourcing.inmemory;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.eventsourcing.AccountEventSourcingTest;
import lt.rieske.accounts.eventsourcing.EventStore;

class InMemoryAccountEventSourcingTest extends AccountEventSourcingTest {

    private final InMemoryEventStore<Account> eventStore = new InMemoryEventStore<>();

    @Override
    protected EventStore<Account> getEventStore() {
        return eventStore;
    }

}
