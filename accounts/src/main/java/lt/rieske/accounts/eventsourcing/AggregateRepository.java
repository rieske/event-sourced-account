package lt.rieske.accounts.eventsourcing;

import java.util.UUID;
import java.util.function.Consumer;

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

    public void create(UUID accountId, Consumer<T> transaction) {
        var eventStream = transactionalEventStream(accountId);
        var aggregate = aggregateFactory.makeAggregate(eventStream);
        transaction.accept(aggregate);
        eventStream.commit();
    }

    public void transact(UUID aggregateId, Consumer<T> transaction) {
        var eventStream = transactionalEventStream(aggregateId);
        var aggregate = aggregateFactory.makeAggregate(eventStream);
        eventStream.replay(aggregate);
        transaction.accept(aggregate);
        eventStream.commit();
    }

    public T query(UUID aggregateId) {
        var eventStream = readOnlyEventStream(aggregateId);
        var aggregate = aggregateFactory.makeAggregate(eventStream);
        eventStream.replay(aggregate);
        return aggregate;
    }

    private TransactionalEventStream<T> transactionalEventStream(UUID aggregateId) {
        return new TransactionalEventStream<>(eventStore, snapshotter, aggregateId);
    }

    private ReplayingEventStream<T> readOnlyEventStream(UUID aggregateId) {
        return new ReplayingEventStream<>(eventStore, aggregateId);
    }
}


