package lt.rieske.accounts.eventstore;

import java.util.Collection;
import java.util.List;
import java.util.UUID;


public interface BlobEventStore {
    void append(Collection<SerializedEvent> serializedEvents, Collection<SerializedEvent> serializedSnapshots, UUID transactionId);

    List<SerializedEvent> getEventsFromSnapshot(UUID aggregateId);
    List<SerializedEvent> getEvents(UUID aggregateId, long fromVersion);
    SerializedEvent loadLatestSnapshot(UUID aggregateId);

    boolean transactionExists(UUID aggregateId, UUID transactionId);
}
