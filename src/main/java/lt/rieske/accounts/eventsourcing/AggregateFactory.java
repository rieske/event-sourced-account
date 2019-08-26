package lt.rieske.accounts.eventsourcing;

import java.util.UUID;

@FunctionalInterface
public interface AggregateFactory<A> {
    A makeAggregate(EventStream eventStream, UUID aggregateId);
}