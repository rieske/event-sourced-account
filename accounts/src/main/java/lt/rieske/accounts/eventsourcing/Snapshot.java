package lt.rieske.accounts.eventsourcing;


import lombok.Value;

import java.util.UUID;

@Value
class Snapshot<T> {
    private final Event<T> snapshotEvent;
    private final long version;

    public void apply(T aggregate) {
        snapshotEvent.apply(aggregate);
    }

    public UUID aggregateId() {
        return snapshotEvent.aggregateId();
    }
}
