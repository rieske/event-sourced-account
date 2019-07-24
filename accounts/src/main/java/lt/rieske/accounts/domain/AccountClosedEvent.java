package lt.rieske.accounts.domain;

import lombok.Value;
import lt.rieske.accounts.eventsourcing.Event;


@Value
public class AccountClosedEvent implements Event<Account> {

    @Override
    public void apply(Account aggregate) {
        aggregate.apply(this);
    }
}
