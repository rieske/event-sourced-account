package lt.rieske.accounts.infrastructure;

import java.util.List;
import java.util.UUID;

public interface BlobEventStore {
    void append(UUID aggregateId, byte[] eventPayload, long sequenceNumber);
    List<byte[]> getEvents(UUID aggregateId, long fromVersion);
    void storeSnapshot(UUID aggregateId, long version, byte[] serializedSnapshot);
    SnapshotBlob loadLatestSnapshot(UUID aggregateId);
}
