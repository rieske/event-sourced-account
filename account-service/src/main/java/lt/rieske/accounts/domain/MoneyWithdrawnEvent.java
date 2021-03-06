package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.Event;


public record MoneyWithdrawnEvent(
        long amountWithdrawn,
        long balance
) implements Event<AccountEventsVisitor> {

    @Override
    public void accept(AccountEventsVisitor visitor) {
        visitor.visit(this);
    }
}
