package lt.rieske.accounts.eventsourcing;

import java.util.List;
import java.util.UUID;

public interface EventStore<T> {

    void append(List<SequencedEvent<T>> uncomittedEvents, SequencedEvent<T> uncomittedSnapshot);

    List<SequencedEvent<T>> getEvents(UUID aggregateId, long fromVersion);
    SequencedEvent<T> loadSnapshot(UUID aggregateId);

    boolean transactionExists(UUID aggregateId, UUID transactionId);
}
