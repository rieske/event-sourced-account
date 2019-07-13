package lt.rieske.accounts.eventsourcing;

import java.util.List;
import java.util.UUID;

public interface EventStore<T> {

    void append(UUID aggregateId, List<SequencedEvent<T>> uncomittedEvents, SequencedEvent<T> uncomittedSnapshot);

    List<Event<T>> getEvents(UUID aggregateId, long fromVersion);
    SequencedEvent<T> loadSnapshot(UUID aggregateId);
}
