package lt.rieske.accounts.domain;

import lombok.Value;
import lt.rieske.accounts.eventsourcing.Event;

import java.util.UUID;

@Value
class AccountOpenedEvent implements Event<Account> {
    private final UUID accountId;
    private final UUID ownerId;

    @Override
    public void apply(Account aggregate) {
        aggregate.apply(this);
    }
}
