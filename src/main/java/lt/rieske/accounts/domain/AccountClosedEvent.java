package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.Event;


public record AccountClosedEvent<T extends AccountEventsVisitor>() implements Event<T> {

    @Override
    public void accept(AccountEventsVisitor visitor) {
        visitor.visit(this);
    }
}
