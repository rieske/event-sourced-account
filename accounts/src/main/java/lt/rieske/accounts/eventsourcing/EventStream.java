package lt.rieske.accounts.eventsourcing;

import java.util.UUID;

public class EventStream<T> {

    private final EventStore<T> eventStore;

    public EventStream(EventStore<T> eventStore) {
        this.eventStore = eventStore;
    }

    public void append(Event<T> event, T aggregate) {
        eventStore.append(event);
        event.apply(aggregate);
    }

    public void replay(T aggregate, UUID aggregateId) {
        var events = eventStore.getEvents(aggregateId);
        if (events.isEmpty()) {
            throw new AggregateNotFoundException(aggregateId);
        }
        events.forEach(event -> event.apply(aggregate));
    }
}
