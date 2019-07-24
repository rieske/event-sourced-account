package lt.rieske.accounts.eventsourcing;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


class ReplayingEventStream<T extends Aggregate> implements EventStream<T> {

    protected final EventStore<T> eventStore;

    final Map<UUID, Long> aggregateVersions = new HashMap<>();

    ReplayingEventStream(EventStore<T> eventStore) {
        this.eventStore = eventStore;
    }

    @Override
    public void append(Event<T> event, T aggregate) {
        throw new UnsupportedOperationException("Can not append to read only event stream");
    }

    void replay(T aggregate) {
        long currentVersion = applySnapshot(aggregate);
        currentVersion = replayEvents(aggregate, currentVersion);

        if (currentVersion == 0) {
            throw new AggregateNotFoundException(aggregate.id());
        }

        aggregateVersions.put(aggregate.id(), currentVersion);
    }

    private long applySnapshot(T aggregate) {
        var snapshot = eventStore.loadSnapshot(aggregate.id());
        if (snapshot != null) {
            snapshot.apply(aggregate);
            return snapshot.getSequenceNumber();
        }
        return 0;
    }

    private long replayEvents(T aggregate, long startingVersion) {
        var events = eventStore.getEvents(aggregate.id(), startingVersion);
        long currentVersion = startingVersion;
        for (var event : events) {
            event.apply(aggregate);
            currentVersion = event.getSequenceNumber();
        }
        return currentVersion;
    }
}
