package lt.rieske.accounts.eventsourcing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


class EventReplayer<A extends EventVisitor<E>, E extends Event> {

    private final EventStore<E> eventStore;
    private final Map<UUID, Long> aggregateVersions = new HashMap<>();
    private final Set<UUID> seenTransactionIds = new HashSet<>();
    // True when a snapshot was applied: event history before the snapshot was not loaded.
    private boolean usedSnapshot;

    EventReplayer(EventStore<E> eventStore) {
        this.eventStore = eventStore;
    }

    void replay(A aggregate, UUID aggregateId) {
        var history = eventStore.loadHistory(aggregateId);
        long currentVersion = 0;
        if (history.snapshot() != null) {
            usedSnapshot = true;
            aggregate.visit(history.snapshot().event());
            currentVersion = history.snapshot().sequenceNumber();
        }
        for (var event : history.events()) {
            if (event.transactionId() != null) {
                seenTransactionIds.add(event.transactionId());
            }
            aggregate.visit(event.event());
            currentVersion = event.sequenceNumber();
        }

        if (currentVersion == 0) {
            throw new AggregateNotFoundException(aggregateId);
        }

        aggregateVersions.put(aggregateId, currentVersion);
    }

    long nextVersion(UUID aggregateId) {
        return aggregateVersions.compute(aggregateId, (id, version) -> version != null ? version + 1 : 1);
    }

    boolean containsTransaction(UUID transactionId) {
        return seenTransactionIds.contains(transactionId);
    }

    /**
     * When false, {@link #containsTransaction} observed the full event history (no snapshot),
     * so a separate store lookup is unnecessary.
     */
    boolean mustCheckTransactionInStore() {
        return usedSnapshot;
    }
}
