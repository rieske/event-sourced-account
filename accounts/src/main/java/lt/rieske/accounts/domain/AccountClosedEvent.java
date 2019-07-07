package lt.rieske.accounts.domain;

import lombok.Value;
import lt.rieske.accounts.eventsourcing.Event;

import java.util.UUID;


@Value
class AccountClosedEvent implements Event<Account> {
    private final UUID accountId;

    @Override
    public UUID aggregateId() {
        return accountId;
    }

    @Override
    public void apply(Account aggregate) {
        aggregate.apply(this);
    }
}
