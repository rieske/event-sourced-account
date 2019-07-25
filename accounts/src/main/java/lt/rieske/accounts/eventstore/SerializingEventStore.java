package lt.rieske.accounts.eventstore;

import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventsourcing.SequencedEvent;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


class SerializingEventStore<T> implements EventStore<T> {

    private final EventSerializer<T> serializer;
    private final BlobEventStore blobStore;

    SerializingEventStore(EventSerializer<T> serializer, BlobEventStore blobStore) {
        this.serializer = serializer;
        this.blobStore = blobStore;
    }

    @Override
    public void append(
            Collection<SequencedEvent<T>> uncomittedEvents,
            Collection<SequencedEvent<T>> uncommittedSnapshots,
            UUID transactionId) {
        var serializedEvents = uncomittedEvents.stream()
                .map(e -> serialize(e, transactionId))
                .collect(Collectors.toUnmodifiableList());
        var serializedSnapshots = uncommittedSnapshots.stream()
                .map(s -> serialize(s, null)).collect(Collectors.toUnmodifiableList());

        blobStore.append(serializedEvents, serializedSnapshots, transactionId);
    }

    @Override
    public List<SequencedEvent<T>> getEvents(UUID aggregateId, long fromVersion) {
        var serializedEvents = blobStore.getEvents(aggregateId, fromVersion);
        return serializer.deserialize(serializedEvents);
    }

    @Override
    public SequencedEvent<T> loadSnapshot(UUID aggregateId) {
        var serializedSnapshot = blobStore.loadLatestSnapshot(aggregateId);
        if (serializedSnapshot == null) {
            return null;
        }
        return new SequencedEvent<>(aggregateId, serializedSnapshot.getSequenceNumber(), null,
                serializer.deserialize(serializedSnapshot.getPayload()));
    }

    @Override
    public boolean transactionExists(UUID aggregateId, UUID transactionId) {
        return blobStore.transactionExists(aggregateId, transactionId);
    }

    private SerializedEvent serialize(SequencedEvent<T> e, UUID transactionId) {
        return new SerializedEvent(e.getAggregateId(), e.getSequenceNumber(), transactionId, serializer.serialize(e.getEvent()));
    }
}
