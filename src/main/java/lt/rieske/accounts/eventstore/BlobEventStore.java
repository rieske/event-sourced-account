package lt.rieske.accounts.eventstore;

import java.util.Collection;
import java.util.List;
import java.util.UUID;


interface BlobEventStore {
    void append(Collection<SerializedEvent> serializedEvents, Collection<SerializedEvent> serializedSnapshots, UUID transactionId);

    List<SerializedEvent> getEvents(UUID aggregateId, long fromVersion);
    SerializedEvent loadLatestSnapshot(UUID aggregateId);

    boolean transactionExists(UUID aggregateId, UUID transactionId);
}
