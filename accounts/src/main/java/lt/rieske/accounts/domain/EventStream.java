package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.Event;

public interface EventStream<T> {
    void append(Event<T> event, T aggregate);
    void replay(T aggregate);
}
