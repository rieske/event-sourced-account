package lt.rieske.accounts.eventsourcing;

import java.util.UUID;

public class AggregateRepository<T> {
    private final EventStore<T> eventStore;
    private final AggregateFactory<T> aggregateFactory;
    private final Snapshotter<T> snapshotter;

    public AggregateRepository(EventStore<T> eventStore, AggregateFactory<T> aggregateFactory) {
        this(eventStore, aggregateFactory, (aggregate, version) -> null);
    }

    public AggregateRepository(EventStore<T> eventStore, AggregateFactory<T> aggregateFactory, Snapshotter<T> snapshotter) {
        this.eventStore = eventStore;
        this.aggregateFactory = aggregateFactory;
        this.snapshotter = snapshotter;
    }

    public T load(UUID aggregateId) {
        var eventStream = eventStream(aggregateId);
        var aggregate = aggregateFactory.makeAggregate(eventStream);
        eventStream.replay(aggregate);
        return aggregate;
    }

    public T create(UUID accountId) {
        return aggregateFactory.makeAggregate(eventStream(accountId));
    }

    private EventSourcedEventStream<T> eventStream(UUID aggregateId) {
        return new EventSourcedEventStream<>(eventStore, snapshotter, aggregateId);
    }
}


