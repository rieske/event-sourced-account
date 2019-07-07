package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.domain.EventStream;

import java.util.UUID;

public class EventSourcedEventStream<T> implements EventStream<T> {

    private final EventStore<T> eventStore;
    private final Snapshotter<T> snapshotter;
    private final UUID aggregateId;

    private long version;

    EventSourcedEventStream(EventStore<T> eventStore, Snapshotter<T> snapshotter, UUID aggregateId) {
        this.eventStore = eventStore;
        this.snapshotter = snapshotter;
        this.aggregateId = aggregateId;
    }

    @Override
    public void append(Event<T> event, T aggregate) {
        long nextSequence = version + 1;
        eventStore.append(event, nextSequence);
        event.apply(aggregate);
        version = nextSequence;
        Event<T> snapshotEvent = snapshotter.takeSnapshot(aggregate, version);
        if (snapshotEvent != null) {
            eventStore.storeSnapshot(snapshotEvent, version);
        }
    }

    void replay(T aggregate) {
        Snapshot<T> snapshot = eventStore.loadSnapshot(aggregateId);
        if (snapshot != null) {
            snapshot.apply(aggregate);
            version = snapshot.getVersion();
        }
        var events = eventStore.getEvents(aggregateId, version);
        if (events.isEmpty() && snapshot == null) {
            throw new AggregateNotFoundException(aggregateId);
        }
        events.forEach(event -> event.apply(aggregate));
        version += events.size();
    }
}
