package lt.rieske.accounts.eventsourcing;

import java.util.UUID;

public interface Event<T> {
    UUID aggregateId();
    void apply(T aggregate);
}
