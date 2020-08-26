package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.Event;

import java.util.UUID;

public record AccountSnapshot(
        UUID accountId,
        UUID ownerId,
        long balance,
        boolean open
) implements Event<AccountEventsVisitor> {

    @Override
    public void accept(AccountEventsVisitor visitor) {
        visitor.visit(this);
    }
}
