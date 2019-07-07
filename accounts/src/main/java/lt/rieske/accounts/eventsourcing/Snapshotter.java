package lt.rieske.accounts.eventsourcing;



public interface Snapshotter<T> {
    /**
     * Takes the snapshot of the current aggregate state.
     * Return null if no snapshot is to be taken for given version.
     *
     * @param aggregate - aggregate to create a snapshot from
     * @param version - current aggregate version - used to determine whether to create snapshot at this point.
     *                The version is incremented on each event, meaning that this is effectively the count of all
     *                events that have been applied to the aggregate up to and including the version number.
     *
     * @return the snapshot event created from the aggregate, or null if no snapshot is to be created for this version
     */
    Event<T> takeSnapshot(T aggregate, long version);
}
