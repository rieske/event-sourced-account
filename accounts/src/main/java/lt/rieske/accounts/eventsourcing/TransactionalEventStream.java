package lt.rieske.accounts.eventsourcing;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionalEventStream<T> extends ReplayingEventStream<T> {

    private final Snapshotter<T> snapshotter;

    private List<SequencedEvent<T>> uncomittedEvents = new ArrayList<>();
    private SequencedEvent<T> uncomittedSnapshot;

    TransactionalEventStream(EventStore<T> eventStore, Snapshotter<T> snapshotter, UUID aggregateId) {
        super(eventStore, aggregateId);
        this.snapshotter = snapshotter;
    }

    @Override
    public void append(Event<T> event, T aggregate) {
        event.apply(aggregate);
        currentVersion++;
        uncomittedEvents.add(new SequencedEvent<>(currentVersion, event));
        var snapshotEvent = snapshotter.takeSnapshot(aggregate, currentVersion);
        if (snapshotEvent != null) {
            uncomittedSnapshot = new SequencedEvent<>(currentVersion, snapshotEvent);
        }
    }

    void commit() {
        eventStore.append(aggregateId, uncomittedEvents, uncomittedSnapshot);
        uncomittedEvents.clear();
        uncomittedSnapshot = null;
    }
}
