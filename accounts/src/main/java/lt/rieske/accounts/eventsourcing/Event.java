package lt.rieske.accounts.eventsourcing;

public interface Event<T> {
    void apply(T aggregate);
}
