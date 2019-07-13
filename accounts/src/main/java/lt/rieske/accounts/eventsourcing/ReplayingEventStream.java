package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.domain.EventStream;

import java.util.UUID;

public class ReplayingEventStream<T> implements EventStream<T> {

    protected final EventStore<T> eventStore;
    protected final UUID aggregateId;

    long currentVersion;

    ReplayingEventStream(EventStore<T> eventStore, UUID aggregateId) {
        this.eventStore = eventStore;
        this.aggregateId = aggregateId;
    }

    @Override
    public void append(Event<T> event, T aggregate) {
        throw new UnsupportedOperationException("Can not append to read only event stream");
    }

    void replay(T aggregate) {
        var snapshot = eventStore.loadSnapshot(aggregateId);
        if (snapshot != null) {
            snapshot.getPayload().apply(aggregate);
            currentVersion = snapshot.getSequenceNumber();
        }
        var events = eventStore.getEvents(aggregateId, currentVersion);
        if (events.isEmpty() && snapshot == null) {
            throw new AggregateNotFoundException(aggregateId);
        }
        events.forEach(event -> event.apply(aggregate));
        currentVersion += events.size();
    }
}
