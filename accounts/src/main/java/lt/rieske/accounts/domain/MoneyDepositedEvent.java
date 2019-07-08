package lt.rieske.accounts.domain;

import lombok.Value;
import lt.rieske.accounts.eventsourcing.Event;


@Value
public class MoneyDepositedEvent implements Event<Account> {
    private final int amountDeposited;
    private final int balance;

    @Override
    public void apply(Account aggregate) {
        aggregate.apply(this);
    }
}
