package lt.rieske.accounts.infrastructure;


import lombok.Value;
import lt.rieske.accounts.eventsourcing.Event;

@Value
class SnapshotBlob {
    private final byte[] snapshotEvent;
    private final long version;
}
