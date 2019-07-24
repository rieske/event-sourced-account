package lt.rieske.accounts.domain;

import lombok.Value;
import lt.rieske.accounts.eventsourcing.Event;

import java.util.UUID;


@Value
public class MoneyWithdrawnEvent implements Event<Account> {
    private final long amountWithdrawn;
    private final long balance;
    private final UUID transactionId;

    @Override
    public void apply(Account aggregate) {
        aggregate.apply(this);
    }
}
