package lt.rieske.accounts.eventsourcing;

import java.util.UUID;


public record SequencedEvent<T>(
        UUID aggregateId,
        long sequenceNumber,
        UUID transactionId,
        Event<T>event
) {
    void apply(T aggregate) {
        event.accept(aggregate);
    }
}
