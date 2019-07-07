package lt.rieske.accounts.eventsourcing;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

public class InMemoryEventStore<T> implements EventStore<T> {
    private final Map<UUID, List<SequencedEvent<T>>> aggregateEvents = new HashMap<>();

    // Synchronized block here simulates what a persistence engine of choice should do - ensure consistency
    // Events can only be written in sequence.
    // One way to ensure this in RDB - primary key on (aggregateId, sequenceNumber)
    @Override
    public synchronized void append(Event<T> event, long sequenceNumber) {
        var currentEvents = aggregateEvents.computeIfAbsent(event.aggregateId(), id -> new ArrayList<>());
        if (!currentEvents.isEmpty()) {
            var lastEvent = currentEvents.get(currentEvents.size() - 1);
            if (sequenceNumber <= lastEvent.getSequenceNumber()) {
                throw new ConcurrentModificationException("Event out of sync, last: " +
                        lastEvent.getSequenceNumber() + ", trying to append: " + sequenceNumber);
            }
        }
        currentEvents.add(new SequencedEvent<>(event, sequenceNumber));
    }

    @Override
    public List<Event<T>> getEvents(UUID aggregateId) {
        return aggregateEvents.getOrDefault(aggregateId, List.of())
                .stream()
                .map(SequencedEvent::getEvent)
                .collect(toList());
    }

    public List<SequencedEvent<T>> getSequencedEvents(UUID aggregateId) {
        return aggregateEvents.getOrDefault(aggregateId, List.of());
    }
}
