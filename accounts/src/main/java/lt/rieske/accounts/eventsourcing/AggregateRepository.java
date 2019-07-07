package lt.rieske.accounts.eventsourcing;

import java.util.UUID;

public abstract class AggregateRepository<T> {
    private final EventStore<T> eventStore;
    private final Snapshotter<T> snapshotter;

    public AggregateRepository(EventStore<T> eventStore) {
        this(eventStore, (aggregate, version) -> null);
    }

    public AggregateRepository(EventStore<T> eventStore, Snapshotter<T> snapshotter) {
        this.eventStore = eventStore;
        this.snapshotter = snapshotter;
    }

    public T load(UUID accountId) {
        var eventStream = eventStream(accountId);
        var account = makeAggregate(eventStream);
        eventStream.replay(account);
        return account;
    }

    public T create(UUID accountId) {
        return makeAggregate(eventStream(accountId));
    }

    protected abstract T makeAggregate(EventStream<T> eventStream);

    private EventStream<T> eventStream(UUID aggregateId) {
        return new EventStream<>(eventStore, snapshotter, aggregateId);
    }
}
