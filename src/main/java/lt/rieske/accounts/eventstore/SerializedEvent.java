package lt.rieske.accounts.eventstore;

import java.util.UUID;


record SerializedEvent(
        UUID aggregateId,
        long sequenceNumber,
        UUID transactionId,
        byte[]payload
) {
}
