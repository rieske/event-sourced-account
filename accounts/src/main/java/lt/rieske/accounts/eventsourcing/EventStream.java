package lt.rieske.accounts.eventsourcing;

import java.util.UUID;

public class EventStream<T> {

    private final EventStore<T> eventStore;
    private final UUID aggregateId;

    public EventStream(EventStore<T> eventStore, UUID aggregateId) {
        this.eventStore = eventStore;
        this.aggregateId = aggregateId;
    }

    public void append(Event<T> event, T aggregate) {
        if (event.aggregateId() != aggregateId) {
            throw aggregateMismatch(event.aggregateId());
        }
        eventStore.append(event);
        event.apply(aggregate);
    }

    public void replay(T aggregate) {
        var events = eventStore.getEvents(aggregateId);
        if (events.isEmpty()) {
            throw new AggregateNotFoundException(aggregateId);
        }
        events.forEach(event -> event.apply(aggregate));
    }

    private IllegalArgumentException aggregateMismatch(UUID eventAggregateId) {
        return new IllegalArgumentException(String.format(
                "Can not apply event, event.aggregateId()=%s, aggregateId=%s", eventAggregateId, aggregateId));
    }
}
