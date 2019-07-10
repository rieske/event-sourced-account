package lt.rieske.accounts.infrastructure;

import java.util.List;
import java.util.UUID;

public interface SqlEventStore {
    void append(UUID aggregateId, long sequenceNumber, byte[] eventPayload);
    List<byte[]> getEvents(UUID aggregateId, long fromVersion);
    void storeSnapshot(UUID aggregateId, long version, byte[] snapshotPayload);
    SnapshotBlob loadLatestSnapshot(UUID aggregateId);
}
