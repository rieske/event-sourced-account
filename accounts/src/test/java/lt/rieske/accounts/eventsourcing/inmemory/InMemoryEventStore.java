package lt.rieske.accounts.eventsourcing.inmemory;

import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventsourcing.SequencedEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

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
