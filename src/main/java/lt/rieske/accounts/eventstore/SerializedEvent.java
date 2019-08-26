package lt.rieske.accounts.eventstore;

import lombok.Value;

import java.util.UUID;


@Value
class SerializedEvent {
    private final UUID aggregateId;
    private final long sequenceNumber;
    private final UUID transactionId;
    private final byte[] payload;
}
