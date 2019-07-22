package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.domain.EventStream;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReplayingEventStream<T extends Aggregate> implements EventStream<T> {

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
        aggregateVersions.put(aggregate.id(), 0L);
        var snapshot = eventStore.loadSnapshot(aggregate.id());
        if (snapshot != null) {
            snapshot.getEvent().apply(aggregate);
            aggregateVersions.put(aggregate.id(), snapshot.getSequenceNumber());
        }
        var events = eventStore.getEvents(aggregate.id(), aggregateVersions.get(aggregate.id()));
        if (events.isEmpty() && snapshot == null) {
            throw new AggregateNotFoundException(aggregate.id());
        }
        events.forEach(event -> event.getEvent().apply(aggregate));
        if (!events.isEmpty()) {
            aggregateVersions.put(aggregate.id(), events.get(events.size() - 1).getSequenceNumber());
        }
    }
}
