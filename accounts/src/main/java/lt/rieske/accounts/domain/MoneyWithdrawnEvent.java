package lt.rieske.accounts.domain;

import lombok.Value;
import lt.rieske.accounts.eventsourcing.Event;


@Value
class MoneyWithdrawnEvent implements Event<Account> {
    private final int amountWithdrawn;
    private final int balance;

    @Override
    public void apply(Account aggregate) {
        aggregate.apply(this);
    }
}
