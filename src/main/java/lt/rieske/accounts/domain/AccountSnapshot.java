package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.Event;

import java.util.UUID;

public record AccountSnapshot<T extends AccountEventsVisitor>(
        UUID accountId,
        UUID ownerId,
        long balance,
        boolean open
) implements Event<T> {

    @Override
    public void accept(AccountEventsVisitor visitor) {
        visitor.visit(this);
    }
}
