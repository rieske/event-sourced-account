package lt.rieske.accounts.domain;

import lombok.Value;
import lt.rieske.accounts.eventsourcing.Event;


@Value
public class AccountClosedEvent<T extends AccountEventsVisitor> implements Event<T> {

    @Override
    public void accept(AccountEventsVisitor visitor) {
        visitor.visit(this);
    }
}
