package lt.rieske.accounts.infrastructure;


import lombok.Value;

@Value
class SnapshotBlob {
    private final long version;
    private final byte[] snapshotEvent;
}
