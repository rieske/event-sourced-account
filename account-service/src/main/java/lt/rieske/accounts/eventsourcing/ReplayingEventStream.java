package lt.rieske.accounts.eventsourcing;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;


class ReplayingEventStream<A extends EventVisitor<E>, E extends Event> implements EventStream<A, E> {

    final EventStore<E> eventStore;

    final Map<UUID, Long> aggregateVersions = new HashMap<>();

    ReplayingEventStream(EventStore<E> eventStore) {
        this.eventStore = eventStore;
    }

    @Override
    public void append(E event, A aggregate, UUID aggregateId) {
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
        SequencedEvent<E> snapshot = eventStore.loadSnapshot(aggregateId);
        if (snapshot != null) {
            aggregate.visit(snapshot.event());
            return snapshot.sequenceNumber();
        }
        return 0;
    }

    private long replayEvents(A aggregate, long startingVersion, UUID aggregateId) {
        var events = eventStore.getEvents(aggregateId, startingVersion);
        var currentVersion = new AtomicLong(startingVersion);
        events.forEach(event -> {
            aggregate.visit(event.event());
            currentVersion.set(event.sequenceNumber());
        });
        return currentVersion.get();
    }
}
