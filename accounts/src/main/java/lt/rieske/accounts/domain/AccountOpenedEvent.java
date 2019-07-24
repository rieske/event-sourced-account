package lt.rieske.accounts.domain;

import lombok.Value;
import lt.rieske.accounts.eventsourcing.Event;

import java.util.UUID;

@Value
public class AccountOpenedEvent implements Event<Account> {
    private final UUID ownerId;
    private final UUID transactionId;

    @Override
    public void apply(Account aggregate) {
        aggregate.apply(this);
    }
}
