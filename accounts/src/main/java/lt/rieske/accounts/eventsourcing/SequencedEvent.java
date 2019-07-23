package lt.rieske.accounts.eventsourcing;

import lombok.Value;

import java.util.UUID;


@Value
public class SequencedEvent<T> {
    private final UUID aggregateId;
    private final long sequenceNumber;
    private final UUID transactionId;
    private final Event<T> event;

    public SequencedEvent(UUID aggregateId, long sequenceNumber, Event<T> event) {
        this.aggregateId = aggregateId;
        this.sequenceNumber = sequenceNumber;
        this.transactionId = event.getTransactionId();
        this.event = event;
    }

    void apply(T aggregate) {
        event.apply(aggregate);
    }
}
