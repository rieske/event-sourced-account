package lt.rieske.accounts.domain;

import lombok.Value;
import lt.rieske.accounts.eventsourcing.Snapshot;

import java.util.UUID;

@Value
class AccountSnapshot implements Snapshot<Account> {

    private final long version;
    private final UUID accountId;
    private final UUID ownerId;
    private final int balance;
    private final boolean open;

    @Override
    public long version() {
        return version;
    }

    @Override
    public UUID aggregateId() {
        return accountId;
    }

    @Override
    public void apply(Account aggregate) {
        aggregate.applySnapshot(this);
    }
}
