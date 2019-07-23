package lt.rieske.accounts.infrastructure;

import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventsourcing.SequencedEvent;
import lt.rieske.accounts.infrastructure.serialization.EventSerializer;

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
    public void append(List<SequencedEvent<T>> uncomittedEvents, SequencedEvent<T> uncomittedSnapshot) {
        var serializedEvents = uncomittedEvents.stream()
                .map(this::serialize)
                .collect(Collectors.toList());
        SerializedEvent serializedSnapshot = null;
        if (uncomittedSnapshot != null) {
            serializedSnapshot = serialize(uncomittedSnapshot);
        }

        blobStore.append(serializedEvents, serializedSnapshot);
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
        return new SequencedEvent<>(aggregateId, serializedSnapshot.getSequenceNumber(),
                serializer.deserialize(serializedSnapshot.getPayload()));
    }

    @Override
    public boolean transactionExists(UUID aggregateId, UUID transactionId) {
        return blobStore.transactionExists(aggregateId, transactionId);
    }

    private SerializedEvent serialize(SequencedEvent<T> e) {
        return new SerializedEvent(e.getAggregateId(), e.getSequenceNumber(), e.getTransactionId(), serializer.serialize(e.getEvent()));
    }
}
