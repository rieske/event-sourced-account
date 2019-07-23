package lt.rieske.accounts.eventsourcing;

import java.util.UUID;

public interface Event<T> {
    void apply(T aggregate);

    default UUID getTransactionId() {
        return null;
    }
}
