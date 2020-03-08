package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.Event;


public record MoneyWithdrawnEvent<T extends AccountEventsVisitor>(
        long amountWithdrawn,
        long balance
) implements Event<T> {

    @Override
    public void accept(AccountEventsVisitor visitor) {
        visitor.visit(this);
    }
}
