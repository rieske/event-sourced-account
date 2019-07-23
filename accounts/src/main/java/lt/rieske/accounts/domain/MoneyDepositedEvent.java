package lt.rieske.accounts.domain;

import lombok.Value;
import lt.rieske.accounts.eventsourcing.Event;

import java.util.UUID;


@Value
public class MoneyDepositedEvent implements Event<Account> {
    private final int amountDeposited;
    private final int balance;
    private final UUID transactionId;

    @Override
    public void apply(Account aggregate) {
        aggregate.apply(this);
    }
}
