package lt.rieske.accounts.eventsourcing;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class AggregateRepository<T extends Aggregate> {
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

    public void create(UUID aggregateId, Consumer<T> transaction) {
        var eventStream = transactionalEventStream();
        var aggregate = aggregateFactory.makeAggregate(eventStream, aggregateId);
        transaction.accept(aggregate);
        eventStream.commit();
    }

    public void transact(UUID aggregateId, Consumer<T> transaction) {
        var eventStream = transactionalEventStream();
        var aggregate = aggregateFactory.makeAggregate(eventStream, aggregateId);
        eventStream.replay(aggregate);
        transaction.accept(aggregate);
        eventStream.commit();
    }

    public void transact(UUID aggregateId1, UUID aggregateId2, BiConsumer<T, T> transaction) {
        var eventStream = transactionalEventStream();
        var aggregate1 = aggregateFactory.makeAggregate(eventStream, aggregateId1);
        eventStream.replay(aggregate1);
        var aggregate2 = aggregateFactory.makeAggregate(eventStream, aggregateId2);
        eventStream.replay(aggregate2);
        transaction.accept(aggregate1, aggregate2);
        eventStream.commit();
    }

    public T query(UUID aggregateId) {
        var eventStream = readOnlyEventStream();
        var aggregate = aggregateFactory.makeAggregate(eventStream, aggregateId);
        eventStream.replay(aggregate);
        return aggregate;
    }

    private TransactionalEventStream<T> transactionalEventStream() {
        return new TransactionalEventStream<>(eventStore, snapshotter);
    }

    private ReplayingEventStream<T> readOnlyEventStream() {
        return new ReplayingEventStream<>(eventStore);
    }
}


