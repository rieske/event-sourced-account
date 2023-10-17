package lt.rieske.accounts.eventsourcing;

import java.util.UUID;

@FunctionalInterface
public interface EventStream<A extends EventVisitor<E>, E extends Event> {
    void append(E event, A aggregate, UUID aggregateId);
}
