package lt.rieske.accounts.domain;

import java.util.UUID;

public record AccountSnapshot(
        UUID accountId,
        UUID ownerId,
        long balance,
        boolean open
) implements AccountEvent {
}
