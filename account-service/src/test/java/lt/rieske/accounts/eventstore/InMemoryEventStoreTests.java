package lt.rieske.accounts.eventstore;

import lt.rieske.accounts.domain.AccountEvent;
import lt.rieske.accounts.eventsourcing.AccountConsistencyTest;
import lt.rieske.accounts.eventsourcing.AccountEventSourcingTest;
import lt.rieske.accounts.eventsourcing.Event;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventsourcing.IdempotencyTest;
import lt.rieske.accounts.eventsourcing.MoneyTransferTest;
import lt.rieske.accounts.eventsourcing.SequencedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryEventStoreTests {

    private final InMemoryEventStore<AccountEvent> eventStore = new InMemoryEventStore<>();

    @Nested
    class InMemoryAccountEventSourcingTest extends AccountEventSourcingTest {

        @Override
        protected EventStore<AccountEvent> getEventStore() {
            return eventStore;
        }
    }

    @Nested
    class InMemoryMoneyTransferTest extends MoneyTransferTest {

        @Override
        protected EventStore<AccountEvent> getEventStore() {
            return eventStore;
        }
    }

    @Nested
    class InMemoryAccountConsistencyTest extends AccountConsistencyTest {

        @Override
        protected EventStore<AccountEvent> getEventStore() {
            return eventStore;
        }

        @AfterEach
        void assertConsistency() {
            for (var aggregateId : aggregateIds()) {
                var events = eventStore.getSequencedEvents(aggregateId);
                for (int i = 0; i < events.size(); i++) {
                    assertThat(events.get(i).sequenceNumber()).isEqualTo(i + 1);
                }
            }
        }
    }

    @Nested
    class InMemoryIdempotencyTest extends IdempotencyTest {

        @Override
        protected EventStore<AccountEvent> getEventStore() {
            return eventStore;
        }
    }

}

class InMemoryEventStore<E extends Event> implements EventStore<E> {
    private final List<SequencedEvent<E>> events = new ArrayList<>();
    private final Map<UUID, SequencedEvent<E>> snapshots = new HashMap<>();

    private final Map<UUID, UUID> aggregateTransactions = new HashMap<>();

    // Synchronized block here simulates what a persistence engine of choice should do - ensure consistency
    // Events can only be written in sequence per aggregate.
    // One way to ensure this in RDB - primary key on (aggregateId, sequenceNumber)
    // Event writes have to happen in a transaction - either all get written or none
    @Override
    public synchronized void append(Collection<SequencedEvent<E>> uncommittedEvents, Collection<SequencedEvent<E>> uncommittedSnapshots,
                                    UUID transactionId) {
        validateConsistency(uncommittedEvents, transactionId);

        uncommittedEvents.forEach(e -> append(e, transactionId));
        uncommittedSnapshots.forEach(s -> snapshots.put(s.aggregateId(), s));
    }

    @Override
    public Stream<SequencedEvent<E>> getEvents(UUID aggregateId, long fromVersion) {
        return events
                .stream()
                .filter(e -> e.aggregateId().equals(aggregateId) && e.sequenceNumber() > fromVersion);
    }

    @Override
    public SequencedEvent<E> loadSnapshot(UUID aggregateId) {
        return snapshots.get(aggregateId);
    }

    @Override
    public boolean transactionExists(UUID aggregateId, UUID transactionId) {
        return transactionId.equals(aggregateTransactions.get(aggregateId));
    }

    List<SequencedEvent<E>> getSequencedEvents(UUID aggregateId) {
        return events
                .stream()
                .filter(e -> e.aggregateId().equals(aggregateId))
                .toList();
    }

    private void append(SequencedEvent<E> event, UUID transactionId) {
        events.add(new SequencedEvent<>(event.aggregateId(), event.sequenceNumber(), transactionId, event.event()));
        aggregateTransactions.put(event.aggregateId(), transactionId);
    }

    private void validateConsistency(Collection<SequencedEvent<E>> uncommittedEvents, UUID transactionId) {
        Map<UUID, Long> aggregateVersions = new HashMap<>();

        uncommittedEvents.forEach(event -> {
            long currentVersion = aggregateVersions.getOrDefault(event.aggregateId(),
                    getLatestAggregateVersion(event.aggregateId()));
            if (transactionExists(event.aggregateId(), transactionId)) {
                throw new ConcurrentModificationException("Duplicate transaction");
            }
            if (event.sequenceNumber() <= currentVersion) {
                throw new ConcurrentModificationException("Event out of sync, last: " +
                        currentVersion + ", trying to append: " + event.sequenceNumber());
            }
            aggregateVersions.put(event.aggregateId(), event.sequenceNumber());
        });
    }

    private long getLatestAggregateVersion(UUID aggregateId) {
        var aggregateEvents = getSequencedEvents(aggregateId);

        if (aggregateEvents.isEmpty()) {
            return 0L;
        }
        return aggregateEvents.get(aggregateEvents.size() - 1).sequenceNumber();
    }
}