package lt.rieske.accounts.eventsourcing;

public interface Snapshotter<T> {
    Snapshot<T> takeSnapshot(T aggregate, long version);
}
