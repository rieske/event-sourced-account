package lt.rieske.accounts.eventsourcing;

import java.util.UUID;

@FunctionalInterface
public interface AggregateFactory<T extends Aggregate> {
    T makeAggregate(EventStream<T> eventStream, UUID aggregateId);
}