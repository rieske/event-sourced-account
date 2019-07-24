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

    public void create(UUID aggregateId, UUID transactionId, Consumer<T> transaction) {
        var eventStream = transactionalEventStream();
        var aggregate = aggregateFactory.makeAggregate(eventStream, aggregateId);
        transaction.accept(aggregate);
        eventStream.commit(transactionId);
    }

    public void transact(UUID aggregateId, UUID transactionId, Consumer<T> transaction) {
        if (eventStore.transactionExists(aggregateId, transactionId)) {
            return;
        }

        var eventStream = transactionalEventStream();
        var aggregate = loadAggregate(eventStream, aggregateId);
        transaction.accept(aggregate);
        eventStream.commit(transactionId);
    }

    public void transact(UUID aggregateId1, UUID aggregateId2, UUID transactionId, BiConsumer<T, T> transaction) {
        if (eventStore.transactionExists(aggregateId1, transactionId) &&
                eventStore.transactionExists(aggregateId2, transactionId)) {
            return;
        }

        var eventStream = transactionalEventStream();
        transaction.accept(loadAggregate(eventStream, aggregateId1), loadAggregate(eventStream, aggregateId2));
        eventStream.commit(transactionId);
    }

    public T query(UUID aggregateId) {
        return loadAggregate(readOnlyEventStream(), aggregateId);
    }

    private T loadAggregate(ReplayingEventStream<T> eventStream, UUID aggregateId) {
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


