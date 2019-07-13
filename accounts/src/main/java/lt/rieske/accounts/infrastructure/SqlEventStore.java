package lt.rieske.accounts.infrastructure;

import java.util.List;
import java.util.UUID;

public interface SqlEventStore {
    void append(UUID aggregateId, List<SerializedEvent> serializedEvents, SerializedEvent serializedSnapshot);

    List<byte[]> getEvents(UUID aggregateId, long fromVersion);
    SnapshotBlob loadLatestSnapshot(UUID aggregateId);
}
