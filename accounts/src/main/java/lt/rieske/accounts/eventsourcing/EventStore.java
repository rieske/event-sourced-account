package lt.rieske.accounts.eventsourcing;

import java.util.List;
import java.util.UUID;

public interface EventStore<T> {
    void append(UUID aggregateId, Event<T> event, long sequenceNumber);
    List<Event<T>> getEvents(UUID aggregateId);
    List<Event<T>> getEvents(UUID aggregateId, long fromVersion);

    void storeSnapshot(UUID aggregateId, Event<T> snapshot, long version);
    Snapshot<T> loadSnapshot(UUID aggregateId);
}
