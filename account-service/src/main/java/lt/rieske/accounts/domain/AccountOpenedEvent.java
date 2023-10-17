package lt.rieske.accounts.domain;

import java.util.UUID;

public record AccountOpenedEvent(UUID ownerId) implements AccountEvent {
}
