package lt.rieske.accounts.domain;

import lombok.Value;
import lt.rieske.accounts.eventsourcing.Event;

import java.util.UUID;

@Value
public class AccountSnapshot implements Event<Account> {

    private final UUID accountId;
    private final UUID ownerId;
    private final long balance;
    private final boolean open;

    private final UUID transactionId;

    @Override
    public void apply(Account aggregate) {
        aggregate.applySnapshot(this);
    }
}
