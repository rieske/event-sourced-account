package lt.rieske.accounts.eventsourcing;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


class EventReplayer<A extends EventVisitor<E>, E extends Event> {

    private final EventStore<E> eventStore;
    private final Map<UUID, Long> aggregateVersions = new HashMap<>();

    EventReplayer(EventStore<E> eventStore) {
        this.eventStore = eventStore;
    }

    void replay(A aggregate, UUID aggregateId) {
        long currentVersion = applySnapshot(aggregate, aggregateId);
        currentVersion = replayEvents(aggregate, currentVersion, aggregateId);

        if (currentVersion == 0) {
            throw new AggregateNotFoundException(aggregateId);
        }

        aggregateVersions.put(aggregateId, currentVersion);
    }

    long nextVersion(UUID aggregateId) {
        return aggregateVersions.compute(aggregateId, (id, version) -> version != null ? version + 1 : 1);
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
        long currentVersion = startingVersion;
        for (var iterator = eventStore.getEvents(aggregateId, startingVersion).iterator(); iterator.hasNext(); ) {
            var event = iterator.next();
            aggregate.visit(event.event());
            currentVersion = event.sequenceNumber();
        }
        return currentVersion;
    }
}
