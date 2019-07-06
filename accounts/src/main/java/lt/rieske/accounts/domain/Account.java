package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.EventStream;

import java.util.UUID;

public class Account {
    private final EventStream<Account> eventStream;

    private UUID accountId;
    private UUID ownerId;
    private int balance;

    Account(EventStream<Account> eventStream) {
        this.eventStream = eventStream;
    }

    public void open(UUID accountId, UUID ownerId) {
        eventStream.append(new AccountOpenedEvent(accountId, ownerId), this);
    }

    public void deposit(int amount) {
        eventStream.append(new MoneyDepositedEvent(accountId, amount), this);
    }

    public UUID id() {
        return accountId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public int balance() {
        return balance;
    }

    void apply(AccountOpenedEvent event) {
        this.accountId = event.getAccountId();
        this.ownerId = event.getOwnerId();
        this.balance = 0;
    }

    void apply(MoneyDepositedEvent event) {
        this.balance += event.getAmount();
    }
}
