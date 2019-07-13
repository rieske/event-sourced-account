package lt.rieske.accounts.infrastructure;


import lombok.Value;

@Value
class SnapshotBlob {
    private final byte[] snapshotEvent;
    private final long version;
}
