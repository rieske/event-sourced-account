package lt.rieske.accounts.infrastructure;

import lt.rieske.accounts.eventsourcing.Event;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventsourcing.Snapshot;
import lt.rieske.accounts.infrastructure.serialization.EventSerializer;

import java.util.List;
import java.util.UUID;

public class SerializingEventStore implements EventStore {

    private final EventSerializer serializer;
    private final BlobEventStore blobStore;

    public SerializingEventStore(EventSerializer serializer, BlobEventStore blobStore) {
        this.serializer = serializer;
        this.blobStore = blobStore;
    }

    @Override
    public void append(UUID aggregateId, Event event, long sequenceNumber) {
        var serializedEvent = serializer.serialize(event);
        blobStore.append(aggregateId, serializedEvent, sequenceNumber);
    }

    @Override
    public List<Event> getEvents(UUID aggregateId, long fromVersion) {
        var serializedEvents = blobStore.getEvents(aggregateId, fromVersion);
        return serializer.deserialize(serializedEvents);
    }

    @Override
    public void storeSnapshot(UUID aggregateId, Event snapshot, long version) {
        var serializedSnapshot = serializer.serialize(snapshot);
        blobStore.storeSnapshot(aggregateId, version, serializedSnapshot);
    }

    @Override
    public Snapshot loadSnapshot(UUID aggregateId) {
        var serializedSnapshot = blobStore.loadLatestSnapshot(aggregateId);
        if (serializedSnapshot == null) {
            return null;
        }
        return snapshot(serializer.deserialize(serializedSnapshot.getSnapshotEvent()), serializedSnapshot.getVersion());
    }

    @SuppressWarnings("unchecked")
    private static Snapshot snapshot(Event e, long version) {
        return new Snapshot(e, version);
    }
}
