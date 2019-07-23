package lt.rieske.accounts.eventsourcing.inmemory;

import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventsourcing.SequencedEvent;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

public class InMemoryEventStore<T> implements EventStore<T> {
    private final Map<UUID, List<SequencedEvent<T>>> aggregateEvents = new HashMap<>();
    private final Map<UUID, SequencedEvent<T>> snapshots = new HashMap<>();

    // Synchronized block here simulates what a persistence engine of choice should do - ensure consistency
    // Events can only be written in sequence.
    // One way to ensure this in RDB - primary key on (aggregateId, sequenceNumber)
    @Override
    public synchronized void append(List<SequencedEvent<T>> uncomittedEvents, SequencedEvent<T> uncomittedSnapshot) {
        validateConsistency(uncomittedEvents);

        uncomittedEvents.forEach(this::append);
        if (uncomittedSnapshot != null) {
            snapshots.put(uncomittedSnapshot.getAggregateId(), uncomittedSnapshot);
        }
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

    List<SequencedEvent<T>> getSequencedEvents(UUID aggregateId) {
        return aggregateEvents.getOrDefault(aggregateId, List.of());
    }

    private void append(SequencedEvent<T> event) {
        var currentEvents = aggregateEvents.computeIfAbsent(event.getAggregateId(), id -> new ArrayList<>());
        currentEvents.add(event);
    }

    private void validateConsistency(List<SequencedEvent<T>> uncomittedEvents) {
        Map<UUID, Long> aggregateVersions = new HashMap<>();

        uncomittedEvents.forEach(event -> {
            long currentVersion = aggregateVersions.getOrDefault(event.getAggregateId(),
                    getLatestAggregateVersion(event.getAggregateId()));

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
