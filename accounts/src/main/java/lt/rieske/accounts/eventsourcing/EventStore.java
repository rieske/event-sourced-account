package lt.rieske.accounts.eventsourcing;

import java.util.List;
import java.util.UUID;

public interface EventStore<T> {

    // Implementations must ensure consistency:
    // - event sequence numbers must be unique per aggregate -
    //   if given sequence number exists for aggregate - we have a concurrent modification - abort.
    //   In such case the client should re-read the current state and retry the operation.
    // - appends must be transactional - either all get written or none
    void append(List<SequencedEvent<T>> uncomittedEvents, SequencedEvent<T> uncomittedSnapshot);

    List<SequencedEvent<T>> getEvents(UUID aggregateId, long fromVersion);
    SequencedEvent<T> loadSnapshot(UUID aggregateId);

    boolean transactionExists(UUID aggregateId, UUID transactionId);
}
