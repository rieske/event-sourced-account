package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.Event;

import java.util.UUID;

public sealed interface AccountEvent extends Event {
    record AccountClosedEvent() implements AccountEvent {
    }

    record AccountOpenedEvent(UUID ownerId) implements AccountEvent {
    }

    record AccountSnapshot(
            UUID accountId,
            UUID ownerId,
            long balance,
            boolean open
    ) implements AccountEvent {
    }

    record MoneyDepositedEvent(long amountDeposited, long balance) implements AccountEvent {
    }

    record MoneyWithdrawnEvent(long amountWithdrawn, long balance) implements AccountEvent {
    }
}

