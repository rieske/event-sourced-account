package lt.rieske.accounts.eventsourcing;

import lombok.Value;

@Value
class SequencedEvent<T> {
    private final Event<T> event;
    private final long sequenceNumber;
}
