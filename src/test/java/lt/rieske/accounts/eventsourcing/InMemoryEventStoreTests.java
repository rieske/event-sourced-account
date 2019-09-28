package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.domain.AccountEventsVisitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        @AfterEach
        void assertConsistency() {
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
    private final List<SequencedEvent<T>> events = new ArrayList<>();
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
    public Stream<SequencedEvent<T>> getEvents(UUID aggregateId, long fromVersion) {
        return events
                .stream()
                .filter(e -> e.getAggregateId().equals(aggregateId) && e.getSequenceNumber() > fromVersion);
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
        return events
                .stream()
                .filter(e -> e.getAggregateId().equals(aggregateId))
                .collect(Collectors.toList());
    }

    private void append(SequencedEvent<T> event, UUID transactionId) {
        events.add(new SequencedEvent<>(event.getAggregateId(), event.getSequenceNumber(), transactionId, event.getEvent()));
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

    private long getLatestAggregateVersion(UUID aggregateId) {
        var aggregateEvents = getSequencedEvents(aggregateId);

        if (aggregateEvents.isEmpty()) {
            return 0L;
        }
        return aggregateEvents.get(aggregateEvents.size() - 1).getSequenceNumber();
    }
}