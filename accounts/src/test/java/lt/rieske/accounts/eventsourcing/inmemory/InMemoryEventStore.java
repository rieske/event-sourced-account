package lt.rieske.accounts.eventsourcing.inmemory;

import lt.rieske.accounts.eventsourcing.Event;
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
    public synchronized void append(UUID aggregateId, List<SequencedEvent<T>> uncomittedEvents, SequencedEvent<T> uncomittedSnapshot) {
        uncomittedEvents.forEach(e -> append(aggregateId, e));
        if (uncomittedSnapshot != null) {
            snapshots.put(aggregateId, uncomittedSnapshot);
        }
    }

    @Override
    public List<Event<T>> getEvents(UUID aggregateId, long fromVersion) {
        return aggregateEvents.getOrDefault(aggregateId, List.of())
                .stream()
                .filter(e -> e.getSequenceNumber() > fromVersion)
                .map(SequencedEvent::getPayload)
                .collect(toList());
    }

    @Override
    public SequencedEvent<T> loadSnapshot(UUID aggregateId) {
        return snapshots.get(aggregateId);
    }

    List<SequencedEvent<T>> getSequencedEvents(UUID aggregateId) {
        return aggregateEvents.getOrDefault(aggregateId, List.of());
    }

    private void append(UUID aggregateId, SequencedEvent<T> event) {
        var currentEvents = aggregateEvents.computeIfAbsent(aggregateId, id -> new ArrayList<>());
        if (!currentEvents.isEmpty()) {
            var lastEvent = currentEvents.get(currentEvents.size() - 1);
            if (event.getSequenceNumber() <= lastEvent.getSequenceNumber()) {
                throw new ConcurrentModificationException("Event out of sync, last: " +
                        lastEvent.getSequenceNumber() + ", trying to append: " + event.getSequenceNumber());
            }
        }
        currentEvents.add(event);
    }
}
