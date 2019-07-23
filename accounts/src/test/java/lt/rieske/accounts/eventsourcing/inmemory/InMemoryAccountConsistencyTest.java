package lt.rieske.accounts.eventsourcing.inmemory;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.eventsourcing.AccountConsistencyTest;
import lt.rieske.accounts.eventsourcing.EventStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class InMemoryAccountConsistencyTest extends AccountConsistencyTest {

    private final InMemoryEventStore<Account> eventStore = new InMemoryEventStore<>();

    @Override
    protected EventStore<Account> getEventStore() {
        return eventStore;
    }

    @Test
    @Override
    protected void accountRemainsConsistentWithConcurrentModifications_noSnapshots() throws InterruptedException {
        super.accountRemainsConsistentWithConcurrentModifications_noSnapshots();
        assertConsistency();
    }

    @Test
    @Override
    protected void accountRemainsConsistentWithConcurrentModifications_withSnapshotting() throws InterruptedException {
        super.accountRemainsConsistentWithConcurrentModifications_withSnapshotting();
        assertConsistency();
    }

    private void assertConsistency() {
        for (var aggregateId : aggregateIds()) {
            var events = eventStore.getSequencedEvents(aggregateId);
            for (int i = 0; i < events.size(); i++) {
                assertThat(events.get(i).getSequenceNumber()).isEqualTo(i + 1);
            }
        }
    }
}
