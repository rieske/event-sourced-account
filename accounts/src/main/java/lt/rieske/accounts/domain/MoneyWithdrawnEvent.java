package lt.rieske.accounts.domain;

import lombok.Value;
import lt.rieske.accounts.eventsourcing.Event;

import java.util.UUID;


@Value
class MoneyWithdrawnEvent implements Event<Account> {
    private final UUID accountId;
    private final int amountWithdrawn;
    private final int balance;

    @Override
    public void apply(Account aggregate) {
        aggregate.apply(this);
    }
}
