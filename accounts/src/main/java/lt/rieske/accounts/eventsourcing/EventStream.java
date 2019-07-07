package lt.rieske.accounts.eventsourcing;

import java.util.UUID;

public class EventStream<T> {

    private final EventStore<T> eventStore;
    private final Snapshotter<T> snapshotter;
    private final UUID aggregateId;

    private long version;

    EventStream(EventStore<T> eventStore, Snapshotter<T> snapshotter, UUID aggregateId) {
        this.eventStore = eventStore;
        this.snapshotter = snapshotter;
        this.aggregateId = aggregateId;
    }

    public void append(Event<T> event, T aggregate) {
        if (event.aggregateId() != aggregateId) {
            throw aggregateMismatch(event.aggregateId());
        }
        long nextSequence = version + 1;
        eventStore.append(event, nextSequence);
        event.apply(aggregate);
        version = nextSequence;
        Snapshot<T> snapshot = snapshotter.takeSnapshot(aggregate, version);
        if (snapshot != null) {
            eventStore.storeSnapshot(snapshot);
        }
    }

    void replay(T aggregate) {
        Snapshot<T> snapshot = eventStore.loadSnapshot(aggregateId);
        if (snapshot != null) {
            snapshot.apply(aggregate);
            version = snapshot.version();
        }
        var events = eventStore.getEvents(aggregateId, version);
        if (events.isEmpty() && snapshot == null) {
            throw new AggregateNotFoundException(aggregateId);
        }
        events.forEach(event -> event.apply(aggregate));
        version += events.size();
    }

    private IllegalArgumentException aggregateMismatch(UUID eventAggregateId) {
        return new IllegalArgumentException(String.format(
                "Can not apply event, event.aggregateId()=%s, aggregateId=%s", eventAggregateId, aggregateId));
    }
}
