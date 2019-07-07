package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.domain.EventStream;

@FunctionalInterface
public interface AggregateFactory<T> {
    T makeAggregate(EventStream<T> eventStream);
}