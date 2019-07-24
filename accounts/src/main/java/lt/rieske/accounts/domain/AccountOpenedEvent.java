package lt.rieske.accounts.domain;

import lombok.Value;
import lt.rieske.accounts.eventsourcing.Event;

import java.util.UUID;

@Value
public class AccountOpenedEvent<T extends AccountEventsVisitor> implements Event<T> {
    private final UUID ownerId;

    @Override
    public void apply(AccountEventsVisitor aggregate) {
        aggregate.visit(this);
    }
}
