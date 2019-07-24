package lt.rieske.accounts.eventsourcing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


class TransactionalEventStream<A extends E, E> extends ReplayingEventStream<A, E> {

    private final Snapshotter<A, E> snapshotter;

    private final List<SequencedEvent<E>> uncommittedEvents = new ArrayList<>();
    private final Map<UUID, SequencedEvent<E>> uncommittedSnapshots = new HashMap<>();

    TransactionalEventStream(EventStore<E> eventStore, Snapshotter<A, E> snapshotter) {
        super(eventStore);
        this.snapshotter = snapshotter;
    }

    @Override
    public void append(Event<E> event, A aggregate, UUID aggregateId) {
        event.apply(aggregate);
        var currentVersion = aggregateVersions.compute(aggregateId, (id, version) -> version != null ? version + 1 : 1);
        uncommittedEvents.add(new SequencedEvent<>(aggregateId, currentVersion, null, event));
        var snapshotEvent = snapshotter.takeSnapshot(aggregate, currentVersion);
        if (snapshotEvent != null) {
            uncommittedSnapshots.put(aggregateId, new SequencedEvent<>(aggregateId, currentVersion, null, snapshotEvent));
        }
    }

    void commit(UUID transactionId) {
        eventStore.append(uncommittedEvents, uncommittedSnapshots.values(), transactionId);
        uncommittedEvents.clear();
        uncommittedSnapshots.clear();
    }

}
