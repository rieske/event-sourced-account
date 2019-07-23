package lt.rieske.accounts.infrastructure;

import lombok.Value;

import java.util.UUID;


@Value
public class SerializedEvent {
    private final UUID aggregateId;
    private final long sequenceNumber;
    private final byte[] payload;
}
