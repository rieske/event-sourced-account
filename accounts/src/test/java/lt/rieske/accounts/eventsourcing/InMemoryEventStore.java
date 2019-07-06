package lt.rieske.accounts.eventsourcing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InMemoryEventStore<T> implements EventStore<T> {
    private final Map<UUID, List<Event<T>>> aggregateEvents = new HashMap<>();

    @Override
    public void append(Event<T> event) {
        aggregateEvents.computeIfAbsent(event.aggregateId(), id -> new ArrayList<>()).add(event);
    }

    @Override
    public List<Event<T>> getEvents(UUID aggregateId) {
        return Collections.unmodifiableList(aggregateEvents.getOrDefault(aggregateId, List.of()));
    }
}
