package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.domain.EventStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class EventSourcedEventStream<T> implements EventStream<T> {

    private final EventStore<T> eventStore;
    private final Snapshotter<T> snapshotter;
    private final UUID aggregateId;

    private List<SequencedEvent<T>> uncomittedEvents = new ArrayList<>();
    private SequencedEvent<T> uncomittedSnapshot;

    private long currentVersion;

    EventSourcedEventStream(EventStore<T> eventStore, Snapshotter<T> snapshotter, UUID aggregateId) {
        this.eventStore = eventStore;
        this.snapshotter = snapshotter;
        this.aggregateId = aggregateId;
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

        commit();
    }

    void replay(T aggregate) {
        var snapshot = eventStore.loadSnapshot(aggregateId);
        if (snapshot != null) {
            snapshot.apply(aggregate);
            currentVersion = snapshot.getVersion();
        }
        var events = eventStore.getEvents(aggregateId, currentVersion);
        if (events.isEmpty() && snapshot == null) {
            throw new AggregateNotFoundException(aggregateId);
        }
        events.forEach(event -> event.apply(aggregate));
        currentVersion += events.size();
    }

    List<SequencedEvent> uncomittedEvents() {
        return Collections.unmodifiableList(uncomittedEvents);
    }

    SequencedEvent uncomittedSnapshot() {
        return uncomittedSnapshot;
    }

    void commit() {
        uncomittedEvents.forEach(e -> eventStore.append(aggregateId, e.getPayload(), e.getSequenceNumber()));
        uncomittedEvents.clear();
        if (uncomittedSnapshot != null) {
            eventStore.storeSnapshot(aggregateId, uncomittedSnapshot.getPayload(), uncomittedSnapshot.getSequenceNumber());
            uncomittedSnapshot = null;
        }
    }
}
