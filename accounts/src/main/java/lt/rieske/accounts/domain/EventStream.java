package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.Aggregate;
import lt.rieske.accounts.eventsourcing.Event;

@FunctionalInterface
public interface EventStream<T extends Aggregate> {
    void append(Event<T> event, T aggregate);
}
