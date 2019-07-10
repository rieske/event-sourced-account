package lt.rieske.accounts.eventsourcing.inmemory;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.domain.AccountSnapshotter;
import lt.rieske.accounts.eventsourcing.AccountConsistencyTest;
import lt.rieske.accounts.eventsourcing.AggregateRepository;
import lt.rieske.accounts.eventsourcing.EventStore;
import org.junit.Test;

import java.util.ConcurrentModificationException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

public class InMemoryAccountConsistencyTest extends AccountConsistencyTest {

    private final InMemoryEventStore<Account> eventStore = new InMemoryEventStore<>();

    @Override
    protected EventStore<Account> getEventStore() {
        return eventStore;
    }

    @Override
    protected Class<? extends RuntimeException> consistencyViolationException() {
        return ConcurrentModificationException.class;
    }

    @Override
    public void accountRemainsConsistentWithConcurrentModifications_noSnapshots() throws InterruptedException {
        super.accountRemainsConsistentWithConcurrentModifications_noSnapshots();
        assertConsistency();
    }

    @Override
    public void accountRemainsConsistentWithConcurrentModifications_withSnapshotting() throws InterruptedException {
        super.accountRemainsConsistentWithConcurrentModifications_withSnapshotting();
        assertConsistency();
    }

    private void assertConsistency() {
        var events = eventStore.getSequencedEvents(aggregateId());
        for (int i = 0; i < depositCount() * threadCount(); i++) {
            assertThat(events.get(i).getSequenceNumber()).isEqualTo(i + 1);
        }
    }
}
