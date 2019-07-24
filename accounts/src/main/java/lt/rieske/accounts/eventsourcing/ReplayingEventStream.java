package lt.rieske.accounts.eventsourcing;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


class ReplayingEventStream<A extends E, E> implements EventStream<A, E> {

    protected final EventStore<E> eventStore;

    final Map<UUID, Long> aggregateVersions = new HashMap<>();

    ReplayingEventStream(EventStore<E> eventStore) {
        this.eventStore = eventStore;
    }

    @Override
    public void append(Event<E> event, A aggregate, UUID aggregateId) {
        throw new UnsupportedOperationException("Can not append to read only event stream");
    }

    void replay(A aggregate, UUID aggregateId) {
        long currentVersion = applySnapshot(aggregate, aggregateId);
        currentVersion = replayEvents(aggregate, currentVersion, aggregateId);

        if (currentVersion == 0) {
            throw new AggregateNotFoundException(aggregateId);
        }

        aggregateVersions.put(aggregateId, currentVersion);
    }

    private long applySnapshot(A aggregate, UUID aggregateId) {
        var snapshot = eventStore.loadSnapshot(aggregateId);
        if (snapshot != null) {
            snapshot.apply(aggregate);
            return snapshot.getSequenceNumber();
        }
        return 0;
    }

    private long replayEvents(A aggregate, long startingVersion, UUID aggregateId) {
        var events = eventStore.getEvents(aggregateId, startingVersion);
        long currentVersion = startingVersion;
        for (var event : events) {
            event.apply(aggregate);
            currentVersion = event.getSequenceNumber();
        }
        return currentVersion;
    }
}
