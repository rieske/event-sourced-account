package lt.rieske.accounts.eventsourcing.inmemory;

import lombok.Value;
import lt.rieske.accounts.eventsourcing.Event;

@Value
class SequencedEvent<T> {
    private final Event<T> event;
    private final long sequenceNumber;
}
