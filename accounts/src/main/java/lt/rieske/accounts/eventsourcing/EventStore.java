package lt.rieske.accounts.eventsourcing;

import java.util.List;
import java.util.UUID;

public interface EventStore<T> {
    void append(Event<T> event);
    List<Event<T>> getEvents(UUID aggregateId);
}
