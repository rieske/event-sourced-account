package lt.rieske.accounts.eventstore;

import java.util.Collection;
import java.util.List;
import java.util.UUID;


public interface BlobEventStore {
    void append(Collection<SerializedEvent> serializedEvents, Collection<SerializedEvent> serializedSnapshots, UUID transactionId);

    List<SerializedEvent> getEvents(UUID aggregateId, long fromVersion);
    SerializedEvent loadLatestSnapshot(UUID aggregateId);

    boolean transactionExists(UUID aggregateId, UUID transactionId);

    /**
     * Load latest snapshot (if any) and events after it in one go.
     * Default chains the two reads; stores may override to use a single connection.
     */
    default AggregateRead load(UUID aggregateId) {
        var snapshot = loadLatestSnapshot(aggregateId);
        long fromVersion = snapshot == null ? 0L : snapshot.sequenceNumber();
        return new AggregateRead(snapshot, getEvents(aggregateId, fromVersion));
    }

    record AggregateRead(SerializedEvent snapshot, List<SerializedEvent> events) {
    }
}
