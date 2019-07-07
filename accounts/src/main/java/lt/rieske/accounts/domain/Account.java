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
        if (amount == 0) {
            return;
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Can not deposit negative amount: " + amount);
        }
        eventStream.append(new MoneyDepositedEvent(accountId, amount, balance + amount), this);
    }

    public void withdraw(int amount) {
        if (amount == 0) {
            return;
        }
        if (balance < amount) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        eventStream.append(new MoneyWithdrawnEvent(accountId, amount, balance - amount), this);
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

    void applySnapshot(AccountSnapshot snapshot) {
        this.accountId = snapshot.getAccountId();
        this.ownerId = snapshot.getOwnerId();
        this.balance = snapshot.getBalance();
    }

    void apply(AccountOpenedEvent event) {
        this.accountId = event.getAccountId();
        this.ownerId = event.getOwnerId();
        this.balance = 0;
    }

    void apply(MoneyDepositedEvent event) {
        this.balance = event.getBalance();
    }

    void apply(MoneyWithdrawnEvent event) {
        this.balance = event.getBalance();
    }
}
