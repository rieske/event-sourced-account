package lt.rieske.accounts.domain;

import lombok.Value;
import lt.rieske.accounts.eventsourcing.Event;


@Value
public class MoneyWithdrawnEvent<T extends AccountEventsVisitor> implements Event<T> {
    private final long amountWithdrawn;
    private final long balance;

    @Override
    public void apply(AccountEventsVisitor aggregate) {
        aggregate.visit(this);
    }
}
