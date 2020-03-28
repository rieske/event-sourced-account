package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.Event;


public record AccountClosedEvent() implements Event<AccountEventsVisitor> {

    @Override
    public void accept(AccountEventsVisitor visitor) {
        visitor.visit(this);
    }
}
