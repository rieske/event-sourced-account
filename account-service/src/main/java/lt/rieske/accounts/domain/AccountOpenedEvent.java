package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.Event;

import java.util.UUID;

public record AccountOpenedEvent(
        UUID ownerId
) implements Event<AccountEventsVisitor> {

    @Override
    public void accept(AccountEventsVisitor visitor) {
        visitor.visit(this);
    }
}
