package lt.rieske.accounts.eventsourcing;

import java.util.UUID;

@FunctionalInterface
public interface AggregateFactory<A extends EventVisitor<E>, E extends Event> {
    A makeAggregate(EventStream<A, E> eventStream, UUID aggregateId);
}