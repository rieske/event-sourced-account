package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.Event;

public sealed interface AccountEvent extends Event permits
        AccountClosedEvent,
        AccountOpenedEvent,
        AccountSnapshot,
        MoneyDepositedEvent,
        MoneyWithdrawnEvent {
}

