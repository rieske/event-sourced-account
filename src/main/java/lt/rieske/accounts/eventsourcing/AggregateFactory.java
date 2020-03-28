package lt.rieske.accounts.eventsourcing;

import java.util.UUID;

@FunctionalInterface
public interface AggregateFactory<A extends E, E> {
    A makeAggregate(EventStream<A, E> eventStream, UUID aggregateId);
}