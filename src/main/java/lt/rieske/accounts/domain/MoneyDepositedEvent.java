package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.Event;


public record MoneyDepositedEvent<T extends AccountEventsVisitor>(
        long amountDeposited,
        long balance
) implements Event<T> {

    @Override
    public void accept(AccountEventsVisitor visitor) {
        visitor.visit(this);
    }
}
