package lt.rieske.accounts.eventsourcing;

@FunctionalInterface
public interface EventStream<T extends Aggregate> {
    void append(Event<T> event, T aggregate);
}
