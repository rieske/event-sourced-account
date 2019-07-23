package lt.rieske.accounts.infrastructure;

import java.util.List;
import java.util.UUID;


interface BlobEventStore {
    void append(List<SerializedEvent> serializedEvents, SerializedEvent serializedSnapshot);

    List<SerializedEvent> getEvents(UUID aggregateId, long fromVersion);
    SerializedEvent loadLatestSnapshot(UUID aggregateId);

    boolean transactionExists(UUID aggregateId, UUID transactionId);
}
