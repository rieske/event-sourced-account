package lt.rieske.accounts.eventsourcing;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


class TransactionalEventStream<T extends Aggregate> extends ReplayingEventStream<T> {

    private final Snapshotter<T> snapshotter;

    private List<SequencedEvent<T>> uncomittedEvents = new ArrayList<>();
    private SequencedEvent<T> uncomittedSnapshot;

    TransactionalEventStream(EventStore<T> eventStore, Snapshotter<T> snapshotter) {
        super(eventStore);
        this.snapshotter = snapshotter;
    }

    @Override
    public void append(Event<T> event, T aggregate) {
        event.apply(aggregate);
        var currentVersion = aggregateVersions.compute(aggregate.id(), (id, version) -> version != null ? version + 1 : 1);
        uncomittedEvents.add(new SequencedEvent<>(aggregate.id(), currentVersion, null, event));
        var snapshotEvent = snapshotter.takeSnapshot(aggregate, currentVersion);
        if (snapshotEvent != null) {
            uncomittedSnapshot = new SequencedEvent<>(aggregate.id(), currentVersion, null, snapshotEvent);
        }
    }

    void commit(UUID transactionId) {
        eventStore.append(uncomittedEvents, uncomittedSnapshot, transactionId);
        uncomittedEvents.clear();
        uncomittedSnapshot = null;
    }

}
