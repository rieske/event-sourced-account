package lt.rieske.accounts.eventsourcing;

public interface Snapshot<T> extends Event<T> {
    long version();
}
