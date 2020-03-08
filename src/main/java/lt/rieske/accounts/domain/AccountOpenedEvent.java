package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.Event;

import java.util.UUID;

public record AccountOpenedEvent<T extends AccountEventsVisitor>(
        UUID ownerId
) implements Event<T> {

    @Override
    public void accept(AccountEventsVisitor visitor) {
        visitor.visit(this);
    }
}
