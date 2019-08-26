package lt.rieske.accounts.eventsourcing;

public interface Event<T> {
    void accept(T visitor);
}
