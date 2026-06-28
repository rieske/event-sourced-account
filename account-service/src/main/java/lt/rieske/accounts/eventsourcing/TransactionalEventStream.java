package lt.rieske.accounts.eventsourcing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


class TransactionalEventStream<A extends EventVisitor<E>, E extends Event> implements EventStream<A, E> {

    private final EventStore<E> eventStore;
    private final EventReplayer<A, E> replayer;
    private final Snapshotter<A, E> snapshotter;

    private final List<SequencedEvent<E>> uncommittedEvents = new ArrayList<>();
    private final Map<UUID, SequencedEvent<E>> uncommittedSnapshots = new HashMap<>();

    TransactionalEventStream(EventStore<E> eventStore, Snapshotter<A, E> snapshotter) {
        this.eventStore = eventStore;
        this.replayer = new EventReplayer<>(eventStore);
        this.snapshotter = snapshotter;
    }

    @Override
    public void append(E event, A aggregate, UUID aggregateId) {
        aggregate.visit(event);
        long currentVersion = replayer.nextVersion(aggregateId);
        uncommittedEvents.add(new SequencedEvent<>(aggregateId, currentVersion, null, event));
        var snapshotEvent = snapshotter.takeSnapshot(aggregate, currentVersion);
        if (snapshotEvent != null) {
            uncommittedSnapshots.put(aggregateId, new SequencedEvent<>(aggregateId, currentVersion, null, snapshotEvent));
        }
    }

    void replay(A aggregate, UUID aggregateId) {
        replayer.replay(aggregate, aggregateId);
    }

    void commit(UUID transactionId) {
        eventStore.append(uncommittedEvents, uncommittedSnapshots.values(), transactionId);
        uncommittedEvents.clear();
        uncommittedSnapshots.clear();
    }

}
