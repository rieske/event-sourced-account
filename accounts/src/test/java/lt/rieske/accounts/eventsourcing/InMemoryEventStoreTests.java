package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.domain.AccountEventsVisitor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

class InMemoryEventStoreTests {

    private final InMemoryEventStore<AccountEventsVisitor> eventStore = new InMemoryEventStore<>();

    @Nested
    class InMemoryAccountEventSourcingTest extends AccountEventSourcingTest {

        @Override
        protected EventStore<AccountEventsVisitor> getEventStore() {
            return eventStore;
        }
    }

    @Nested
    class InMemoryMoneyTransferTest extends MoneyTransferTest {

        @Override
        protected EventStore<AccountEventsVisitor> getEventStore() {
            return eventStore;
        }
    }

    @Nested
    class InMemoryAccountConsistencyTest extends AccountConsistencyTest {

        @Override
        protected EventStore<AccountEventsVisitor> getEventStore() {
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

    @Nested
    class InMemoryIdempotencyTest extends IdempotencyTest {

        @Override
        protected EventStore<AccountEventsVisitor> getEventStore() {
            return eventStore;
        }
    }

}

class InMemoryEventStore<T> implements EventStore<T> {
    private final Map<UUID, List<SequencedEvent<T>>> aggregateEvents = new HashMap<>();
    private final Map<UUID, SequencedEvent<T>> snapshots = new HashMap<>();

    private final Map<UUID, UUID> aggregateTransactions = new HashMap<>();

    // Synchronized block here simulates what a persistence engine of choice should do - ensure consistency
    // Events can only be written in sequence per aggregate.
    // One way to ensure this in RDB - primary key on (aggregateId, sequenceNumber)
    // Event writes have to happen in a transaction - either all get written or none
    @Override
    public synchronized void append(Collection<SequencedEvent<T>> uncommittedEvents, Collection<SequencedEvent<T>> uncommittedSnapshots,
                                    UUID transactionId) {
        validateConsistency(uncommittedEvents, transactionId);

        uncommittedEvents.forEach(e -> append(e, transactionId));
        uncommittedSnapshots.forEach(s -> snapshots.put(s.getAggregateId(), s));
    }

    @Override
    public List<SequencedEvent<T>> getEvents(UUID aggregateId, long fromVersion) {
        return aggregateEvents.getOrDefault(aggregateId, List.of())
                .stream()
                .filter(e -> e.getSequenceNumber() > fromVersion)
                .collect(toList());
    }

    @Override
    public SequencedEvent<T> loadSnapshot(UUID aggregateId) {
        return snapshots.get(aggregateId);
    }

    @Override
    public boolean transactionExists(UUID aggregateId, UUID transactionId) {
        return transactionId.equals(aggregateTransactions.get(aggregateId));
    }

    List<SequencedEvent<T>> getSequencedEvents(UUID aggregateId) {
        return aggregateEvents.getOrDefault(aggregateId, List.of());
    }

    private void append(SequencedEvent<T> event, UUID transactionId) {
        var currentEvents = aggregateEvents.computeIfAbsent(event.getAggregateId(), id -> new ArrayList<>());
        currentEvents.add(new SequencedEvent<>(event.getAggregateId(), event.getSequenceNumber(), transactionId, event.getEvent()));

        aggregateTransactions.put(event.getAggregateId(), transactionId);
    }

    private void validateConsistency(Collection<SequencedEvent<T>> uncomittedEvents, UUID transactionId) {
        Map<UUID, Long> aggregateVersions = new HashMap<>();

        uncomittedEvents.forEach(event -> {
            long currentVersion = aggregateVersions.getOrDefault(event.getAggregateId(),
                    getLatestAggregateVersion(event.getAggregateId()));
            if (transactionExists(event.getAggregateId(), transactionId)) {
                throw new ConcurrentModificationException("Duplicate transaction");
            }
            if (event.getSequenceNumber() <= currentVersion) {
                throw new ConcurrentModificationException("Event out of sync, last: " +
                        currentVersion + ", trying to append: " + event.getSequenceNumber());
            }
            aggregateVersions.put(event.getAggregateId(), event.getSequenceNumber());
        });
    }

    private Long getLatestAggregateVersion(UUID aggregateId) {
        var currentEvents = aggregateEvents.get(aggregateId);
        if (currentEvents == null || currentEvents.isEmpty()) {
            return 0L;
        }
        return currentEvents.get(currentEvents.size() - 1).getSequenceNumber();
    }
}