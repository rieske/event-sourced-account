package lt.rieske.accounts.infrastructure;

import lt.rieske.accounts.eventsourcing.Event;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventsourcing.SequencedEvent;
import lt.rieske.accounts.eventsourcing.Snapshot;
import lt.rieske.accounts.infrastructure.serialization.EventSerializer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SerializingEventStore<T> implements EventStore<T> {

    private final EventSerializer<T> serializer;
    private final SqlEventStore blobStore;

    public SerializingEventStore(EventSerializer<T> serializer, SqlEventStore blobStore) {
        this.serializer = serializer;
        this.blobStore = blobStore;
    }

    @Override
    public void append(UUID aggregateId, List<SequencedEvent<T>> uncomittedEvents, SequencedEvent<T> uncomittedSnapshot) {
        var serializedEvents = uncomittedEvents.stream()
                .map(e -> new SerializedEvent(e.getSequenceNumber(), serializer.serialize(e.getPayload())))
                .collect(Collectors.toList());
        SerializedEvent serializedSnapshot = null;
        if (uncomittedSnapshot != null) {
            serializedSnapshot = new SerializedEvent(
                    uncomittedSnapshot.getSequenceNumber(),
                    serializer.serialize(uncomittedSnapshot.getPayload()));
        }

        blobStore.append(aggregateId, serializedEvents, serializedSnapshot);
    }

    @Override
    public List<Event<T>> getEvents(UUID aggregateId, long fromVersion) {
        var serializedEvents = blobStore.getEvents(aggregateId, fromVersion);
        return serializer.deserialize(serializedEvents);
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
