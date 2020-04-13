package lt.rieske.accounts.eventstore;

import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventsourcing.SequencedEvent;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;


class SerializingEventStore<T> implements EventStore<T> {

    private final EventSerializer<T> serializer;
    private final BlobEventStore blobStore;

    SerializingEventStore(EventSerializer<T> serializer, BlobEventStore blobStore) {
        this.serializer = serializer;
        this.blobStore = blobStore;
    }

    @Override
    public void append(
            Collection<SequencedEvent<T>> uncommittedEvents,
            Collection<SequencedEvent<T>> uncommittedSnapshots,
            UUID transactionId) {
        var serializedEvents = uncommittedEvents.stream()
                .map(e -> serialize(e, transactionId))
                .collect(Collectors.toUnmodifiableList());
        var serializedSnapshots = uncommittedSnapshots.stream()
                .map(s -> serialize(s, null))
                .collect(Collectors.toUnmodifiableList());

        blobStore.append(serializedEvents, serializedSnapshots, transactionId);
    }

    @Override
    public Stream<SequencedEvent<T>> getEventsFromSnapshot(UUID aggregateId) {
        var serializedEvents = blobStore.getEventsFromSnapshot(aggregateId);
        return deserialize(serializedEvents);
    }

    @Override
    public Stream<SequencedEvent<T>> getEvents(UUID aggregateId, long fromVersion) {
        var serializedEvents = blobStore.getEvents(aggregateId, fromVersion);
        return deserialize(serializedEvents);
    }

    @Override
    public SequencedEvent<T> loadSnapshot(UUID aggregateId) {
        var serializedSnapshot = blobStore.loadLatestSnapshot(aggregateId);
        if (serializedSnapshot == null) {
            return null;
        }
        return deserialize(serializedSnapshot);
    }

    private Stream<SequencedEvent<T>> deserialize(List<SerializedEvent> serializedEvents) {
        return serializedEvents.stream()
                .map(this::deserialize);
    }

    private SequencedEvent<T> deserialize(SerializedEvent serializedEvent) {
        return new SequencedEvent<>(
                serializedEvent.aggregateId(),
                serializedEvent.sequenceNumber(),
                serializedEvent.transactionId(),
                serializer.deserialize(serializedEvent.payload()));
    }

    @Override
    public boolean transactionExists(UUID aggregateId, UUID transactionId) {
        return blobStore.transactionExists(aggregateId, transactionId);
    }

    private SerializedEvent serialize(SequencedEvent<T> event, UUID transactionId) {
        return new SerializedEvent(
                event.aggregateId(),
                event.sequenceNumber(),
                transactionId,
                serializer.serialize(event.event()));
    }
}
