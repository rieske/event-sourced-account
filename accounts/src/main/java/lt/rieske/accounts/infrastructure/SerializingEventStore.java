package lt.rieske.accounts.infrastructure;

import lt.rieske.accounts.eventsourcing.Event;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventsourcing.Snapshot;
import lt.rieske.accounts.infrastructure.serialization.EventSerializer;

import java.util.List;
import java.util.UUID;

public class SerializingEventStore<T> implements EventStore<T> {

    private final EventSerializer<T> serializer;
    private final SqlEventStore blobStore;

    public SerializingEventStore(EventSerializer<T> serializer, SqlEventStore blobStore) {
        this.serializer = serializer;
        this.blobStore = blobStore;
    }

    @Override
    public void append(UUID aggregateId, Event event, long sequenceNumber) {
        var serializedEvent = serializer.serialize(event);
        blobStore.append(aggregateId, sequenceNumber, serializedEvent);
    }

    @Override
    public List<Event<T>> getEvents(UUID aggregateId, long fromVersion) {
        var serializedEvents = blobStore.getEvents(aggregateId, fromVersion);
        return serializer.deserialize(serializedEvents);
    }

    @Override
    public void storeSnapshot(UUID aggregateId, Event snapshot, long version) {
        var serializedSnapshot = serializer.serialize(snapshot);
        blobStore.storeSnapshot(aggregateId, version, serializedSnapshot);
    }

    @Override
    public Snapshot<T> loadSnapshot(UUID aggregateId) {
        var serializedSnapshot = blobStore.loadLatestSnapshot(aggregateId);
        if (serializedSnapshot == null) {
            return null;
        }
        return snapshot(serializer.deserialize(serializedSnapshot.getSnapshotEvent()), serializedSnapshot.getVersion());
    }

    @SuppressWarnings("unchecked")
    private static <T> Snapshot<T> snapshot(Event e, long version) {
        return new Snapshot<>(e, version);
    }
}
