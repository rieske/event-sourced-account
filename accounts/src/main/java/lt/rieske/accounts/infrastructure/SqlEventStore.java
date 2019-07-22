package lt.rieske.accounts.infrastructure;

import java.util.List;
import java.util.UUID;

public interface SqlEventStore {
    void append(List<SerializedEvent> serializedEvents, SerializedEvent serializedSnapshot);

    List<SerializedEvent> getEvents(UUID aggregateId, long fromVersion);
    SerializedEvent loadLatestSnapshot(UUID aggregateId);
}
