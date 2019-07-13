package lt.rieske.accounts.infrastructure;

import lombok.Value;

@Value
public class SerializedEvent {
    private final long sequenceNumber;
    private final byte[] payload;
}
