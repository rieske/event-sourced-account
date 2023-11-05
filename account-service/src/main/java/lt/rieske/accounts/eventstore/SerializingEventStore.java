package lt.rieske.accounts.eventstore;

import lt.rieske.accounts.eventsourcing.Event;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventsourcing.SequencedEvent;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;


class SerializingEventStore<E extends Event> implements EventStore<E> {

    private final EventSerializer<E> serializer;
    private final BlobEventStore blobStore;

    SerializingEventStore(EventSerializer<E> serializer, BlobEventStore blobStore) {
        this.serializer = serializer;
        this.blobStore = blobStore;
    }

    @Override
    public void append(
            Collection<SequencedEvent<E>> uncommittedEvents,
            Collection<SequencedEvent<E>> uncommittedSnapshots,
            UUID transactionId) {
        var serializedEvents = uncommittedEvents.stream()
                .map(e -> serialize(e, transactionId)).toList();
        var serializedSnapshots = uncommittedSnapshots.stream()
                .map(s -> serialize(s, null)).toList();

        blobStore.append(serializedEvents, serializedSnapshots, transactionId);
    }

    @Override
    public Stream<SequencedEvent<E>> getEvents(UUID aggregateId, long fromVersion) {
        var serializedEvents = blobStore.getEvents(aggregateId, fromVersion);
        return deserialize(serializedEvents);
    }

    @Override
    public SequencedEvent<E> loadSnapshot(UUID aggregateId) {
        var serializedSnapshot = blobStore.loadLatestSnapshot(aggregateId);
        if (serializedSnapshot == null) {
            return null;
        }
        return deserialize(serializedSnapshot);
    }

    private Stream<SequencedEvent<E>> deserialize(List<SerializedEvent> serializedEvents) {
        return serializedEvents.stream()
                .map(this::deserialize);
    }

    private SequencedEvent<E> deserialize(SerializedEvent serializedEvent) {
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

    private SerializedEvent serialize(SequencedEvent<E> event, UUID transactionId) {
        return new SerializedEvent(
                event.aggregateId(),
                event.sequenceNumber(),
                transactionId,
                serializer.serialize(event.event()));
    }
}
