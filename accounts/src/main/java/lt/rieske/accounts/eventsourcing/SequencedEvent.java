package lt.rieske.accounts.eventsourcing;

import lombok.Value;

import java.util.UUID;


@Value
public class SequencedEvent<T> {
    private final UUID aggregateId;
    private final long sequenceNumber;
    private final Event<T> payload;
}
