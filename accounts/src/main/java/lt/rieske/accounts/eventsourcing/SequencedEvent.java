package lt.rieske.accounts.eventsourcing;

import lombok.Value;


@Value
public class SequencedEvent<T> {
    private final long sequenceNumber;
    private final Event<T> payload;
}
