package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.domain.EventStream;

import java.util.UUID;

@FunctionalInterface
public interface AggregateFactory<T extends Aggregate> {
    T makeAggregate(EventStream<T> eventStream, UUID aggregateId);
}