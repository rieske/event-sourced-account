package lt.rieske.accounts.domain;

import lombok.Value;
import lt.rieske.accounts.eventsourcing.Event;

import java.util.UUID;

@Value
public class AccountSnapshot<T extends AccountEventsVisitor> implements Event<T> {

    private final UUID accountId;
    private final UUID ownerId;
    private final long balance;
    private final boolean open;

    @Override
    public void accept(AccountEventsVisitor visitor) {
        visitor.visit(this);
    }
}
