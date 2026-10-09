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

    /**
     * Snapshot (nullable) plus events after it. Default implementation issues two reads;
     * stores may collapse them into one round-trip.
     */
    default AggregateHistory<E> loadHistory(UUID aggregateId) {
        var snapshot = loadSnapshot(aggregateId);
        long fromVersion = snapshot == null ? 0L : snapshot.sequenceNumber();
        return new AggregateHistory<>(snapshot, getEvents(aggregateId, fromVersion).toList());
    }

    record AggregateHistory<E extends Event>(SequencedEvent<E> snapshot, java.util.List<SequencedEvent<E>> events) {
    }
}
