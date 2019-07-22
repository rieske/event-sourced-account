package lt.rieske.accounts.eventsourcing;

import java.util.UUID;

public interface Aggregate {
    UUID id();
}
