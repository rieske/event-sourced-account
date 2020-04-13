package lt.rieske.accounts.eventsourcing;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;


class ReplayingEventStream<A extends E, E> implements EventStream<A, E> {

    final EventStore<E> eventStore;

    final Map<UUID, Long> aggregateVersions = new HashMap<>();

    ReplayingEventStream(EventStore<E> eventStore) {
        this.eventStore = eventStore;
    }

    @Override
    public void append(Event<E> event, A aggregate, UUID aggregateId) {
        throw new UnsupportedOperationException("Can not append to read only event stream");
    }

    void replay(A aggregate, UUID aggregateId) {
        var currentVersion = new AtomicLong();
        eventStore.getEventsFromSnapshot(aggregateId).forEach(event -> {
            event.apply(aggregate);
            currentVersion.set(event.sequenceNumber());
        });

        if (currentVersion.get() == 0) {
            throw new AggregateNotFoundException(aggregateId);
        }

        aggregateVersions.put(aggregateId, currentVersion.get());
    }

}
