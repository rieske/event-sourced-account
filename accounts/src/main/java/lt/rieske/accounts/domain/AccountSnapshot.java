package lt.rieske.accounts.domain;

import lombok.Value;
import lt.rieske.accounts.eventsourcing.Event;

import java.util.UUID;

@Value
public class AccountSnapshot implements Event<Account> {

    private final UUID accountId;
    private final UUID ownerId;
    private final int balance;
    private final boolean open;

    @Override
    public void apply(Account aggregate) {
        aggregate.applySnapshot(this);
    }
}
