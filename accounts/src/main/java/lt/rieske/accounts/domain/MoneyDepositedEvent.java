package lt.rieske.accounts.domain;

import lombok.Value;
import lt.rieske.accounts.eventsourcing.Event;


@Value
public class MoneyDepositedEvent<T extends AccountEventsVisitor> implements Event<T> {
    private final long amountDeposited;
    private final long balance;

    @Override
    public void apply(AccountEventsVisitor aggregate) {
        aggregate.visit(this);
    }
}
