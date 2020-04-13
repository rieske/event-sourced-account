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
                    assertThat(events.get(i).sequenceNumber()).isEqualTo(i + 1);
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
        uncommittedSnapshots.forEach(s -> snapshots.put(s.aggregateId(), s));
    }

    @Override
    public Stream<SequencedEvent<T>> getEventsFromSnapshot(UUID aggregateId) {
        SequencedEvent<T> snapshot = snapshots.get(aggregateId);
        if (snapshot == null) {
            return events.stream()
                    .filter(e -> e.aggregateId().equals(aggregateId));
        } else {
            List<SequencedEvent<T>> aggregateEvents = new ArrayList<>();
            aggregateEvents.add(snapshot);
            aggregateEvents.addAll(getEvents(aggregateId, snapshot.sequenceNumber()).collect(Collectors.toList()));
            return aggregateEvents.stream();
        }
    }

    @Override
    public Stream<SequencedEvent<T>> getEvents(UUID aggregateId, long fromVersion) {
        return events
                .stream()
                .filter(e -> e.aggregateId().equals(aggregateId) && e.sequenceNumber() > fromVersion);
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
                .filter(e -> e.aggregateId().equals(aggregateId))
                .collect(Collectors.toList());
    }

    private void append(SequencedEvent<T> event, UUID transactionId) {
        events.add(new SequencedEvent<>(event.aggregateId(), event.sequenceNumber(), transactionId, event.event()));
        aggregateTransactions.put(event.aggregateId(), transactionId);
    }

    private void validateConsistency(Collection<SequencedEvent<T>> uncomittedEvents, UUID transactionId) {
        Map<UUID, Long> aggregateVersions = new HashMap<>();

        uncomittedEvents.forEach(event -> {
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