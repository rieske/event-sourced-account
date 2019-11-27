package lt.rieske.accounts.eventsourcing;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class AggregateRepository<A extends E, E> {
    private final EventStore<E> eventStore;
    private final AggregateFactory<A> aggregateFactory;
    private final Snapshotter<A, E> snapshotter;

    public AggregateRepository(EventStore<E> eventStore, AggregateFactory<A> aggregateFactory) {
        this(eventStore, aggregateFactory, (aggregate, version) -> null);
    }

    public AggregateRepository(EventStore<E> eventStore, AggregateFactory<A> aggregateFactory, Snapshotter<A, E> snapshotter) {
        this.eventStore = eventStore;
        this.aggregateFactory = aggregateFactory;
        this.snapshotter = snapshotter;
    }

    public void create(UUID aggregateId, UUID transactionId, Consumer<A> transaction) {
        var eventStream = transactionalEventStream();
        var aggregate = aggregateFactory.makeAggregate(eventStream, aggregateId);
        transaction.accept(aggregate);
        eventStream.commit(transactionId);
    }

    public void transact(UUID aggregateId, UUID transactionId, Consumer<A> transaction) {
        // aggregate has to be loaded fist thing, before checking for transaction existence - otherwise we might operate on stale data
        var eventStream = transactionalEventStream();
        var aggregate = loadAggregate(eventStream, aggregateId);

        if (eventStore.transactionExists(aggregateId, transactionId)) {
            return;
        }

        transaction.accept(aggregate);
        eventStream.commit(transactionId);
    }

    public void transact(UUID aggregateId1, UUID aggregateId2, UUID transactionId, BiConsumer<A, A> transaction) {
        // aggregates have to be loaded fist thing, before checking for transaction existence - otherwise we might operate on stale data
        var eventStream = transactionalEventStream();
        var aggregate1 = loadAggregate(eventStream, aggregateId1);
        var aggregate2 = loadAggregate(eventStream, aggregateId2);

        if (eventStore.transactionExists(aggregateId1, transactionId) ||
                eventStore.transactionExists(aggregateId2, transactionId)) {
            return;
        }

        transaction.accept(aggregate1, aggregate2);
        eventStream.commit(transactionId);
    }

    public A query(UUID aggregateId) {
        return loadAggregate(readOnlyEventStream(), aggregateId);
    }

    private A loadAggregate(ReplayingEventStream<A, E> eventStream, UUID aggregateId) {
        var aggregate = aggregateFactory.makeAggregate(eventStream, aggregateId);
        eventStream.replay(aggregate, aggregateId);
        return aggregate;
    }

    private TransactionalEventStream<A, E> transactionalEventStream() {
        return new TransactionalEventStream<>(eventStore, snapshotter);
    }

    private ReplayingEventStream<A, E> readOnlyEventStream() {
        return new ReplayingEventStream<>(eventStore);
    }
}


