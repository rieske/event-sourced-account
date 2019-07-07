package lt.rieske.accounts.eventsourcing;

import java.util.UUID;

public class AggregateNotFoundException extends RuntimeException {

    public AggregateNotFoundException(UUID aggregateId) {
        super("Aggregate not found, aggregateId: " + aggregateId);
    }
}
