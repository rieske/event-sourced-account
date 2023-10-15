package lt.rieske.accounts.eventsourcing;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;

public interface EventStore<E extends Event> {

    // Implementations must ensure consistency:
    // - event sequence numbers must be unique per aggregate -
    //   if given sequence number exists for aggregate - we have a concurrent modification - abort.
    //   In such case the client should re-read the current state and retry the operation.
    // - appends must be transactional - either all get written or none
    void append(Collection<SequencedEvent<E>> uncommittedEvents, Collection<SequencedEvent<E>> uncommittedSnapshots, UUID transactionId);

    Stream<SequencedEvent<E>> getEvents(UUID aggregateId, long fromVersion);
    SequencedEvent<E> loadSnapshot(UUID aggregateId);

    boolean transactionExists(UUID aggregateId, UUID transactionId);
}
