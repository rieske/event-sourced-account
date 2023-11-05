package lt.rieske.accounts.eventsourcing;

import java.util.UUID;


public record SequencedEvent<T>(
        UUID aggregateId,
        long sequenceNumber,
        UUID transactionId,
        T event
) {
}
