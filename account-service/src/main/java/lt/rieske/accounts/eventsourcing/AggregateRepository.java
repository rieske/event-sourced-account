package lt.rieske.accounts.eventsourcing;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class AggregateRepository<A extends EventVisitor<E>, E extends Event> {
    private final EventStore<E> eventStore;
    private final AggregateFactory<A, E> aggregateFactory;
    private final Snapshotter<A, E> snapshotter;

    public AggregateRepository(EventStore<E> eventStore, AggregateFactory<A, E> aggregateFactory) {
        this(eventStore, aggregateFactory, (aggregate, version) -> null);
    }

    public AggregateRepository(EventStore<E> eventStore, AggregateFactory<A, E> aggregateFactory, Snapshotter<A, E> snapshotter) {
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

        if (isDuplicateTransaction(eventStream, aggregateId, transactionId)) {
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

        if (isDuplicateTransaction(eventStream, aggregateId1, transactionId)
                || isDuplicateTransaction(eventStream, aggregateId2, transactionId)) {
            return;
        }

        transaction.accept(aggregate1, aggregate2);
        eventStream.commit(transactionId);
    }

    /**
     * Prefer transaction ids already observed while replaying events (no extra DB round-trip).
     * Fall back to the store only when a snapshot truncated history.
     */
    private boolean isDuplicateTransaction(TransactionalEventStream<A, E> eventStream, UUID aggregateId, UUID transactionId) {
        if (eventStream.containsTransaction(transactionId)) {
            return true;
        }
        return eventStream.mustCheckTransactionInStore()
                && eventStore.transactionExists(aggregateId, transactionId);
    }

    public A query(UUID aggregateId) {
        EventStream<A, E> readOnlyStream = (event, aggregate, id) -> {
            throw new UnsupportedOperationException("Can not append to read only event stream");
        };
        var aggregate = aggregateFactory.makeAggregate(readOnlyStream, aggregateId);
        new EventReplayer<>(eventStore).replay(aggregate, aggregateId);
        return aggregate;
    }

    private A loadAggregate(TransactionalEventStream<A, E> eventStream, UUID aggregateId) {
        var aggregate = aggregateFactory.makeAggregate(eventStream, aggregateId);
        eventStream.replay(aggregate, aggregateId);
        return aggregate;
    }

    private TransactionalEventStream<A, E> transactionalEventStream() {
        return new TransactionalEventStream<>(eventStore, snapshotter);
    }
}
