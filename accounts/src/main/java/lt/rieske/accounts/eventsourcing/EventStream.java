package lt.rieske.accounts.eventsourcing;

import java.util.UUID;

@FunctionalInterface
public interface EventStream<A extends E, E> {
    void append(Event<E> event, A aggregate, UUID aggregateId);
}
