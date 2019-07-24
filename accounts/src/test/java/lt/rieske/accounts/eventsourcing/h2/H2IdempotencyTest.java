package lt.rieske.accounts.eventsourcing.h2;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventsourcing.IdempotencyTest;
import lt.rieske.accounts.eventstore.Configuration;

class H2IdempotencyTest extends IdempotencyTest {

    private static final H2 H2 = new H2();

    private EventStore<Account> eventStore = Configuration.accountEventStore(H2.dataSource());

    @Override
    protected EventStore<Account> getEventStore() {
        return eventStore;
    }

}
