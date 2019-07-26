package lt.rieske.accounts.eventsourcing;

import lombok.Value;

import java.util.UUID;


@Value
public class SequencedEvent<T> {
    private final UUID aggregateId;
    private final long sequenceNumber;
    private final UUID transactionId;
    private final Event<T> event;

    void apply(T aggregate) {
        event.apply(aggregate);
    }
}
