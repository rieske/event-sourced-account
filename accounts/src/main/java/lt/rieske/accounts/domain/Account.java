package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.Aggregate;

import java.util.UUID;

public class Account implements Aggregate {
    private final EventStream<Account> eventStream;

    private final UUID accountId;

    private UUID ownerId;
    private int balance;
    private boolean open;

    public Account(EventStream<Account> eventStream, UUID accountId) {
        this.eventStream = eventStream;
        this.accountId = accountId;
    }

    public void open(UUID ownerId) {
        eventStream.append(new AccountOpenedEvent(ownerId), this);
    }

    public void deposit(int amount) {
        if (amount == 0) {
            return;
        }
        requireOpenAccount();
        if (amount < 0) {
            throw new IllegalArgumentException("Can not deposit negative amount: " + amount);
        }
        eventStream.append(new MoneyDepositedEvent(amount, balance + amount), this);
    }

    public void withdraw(int amount) {
        if (amount == 0) {
            return;
        }
        requireOpenAccount();
        if (balance < amount) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        eventStream.append(new MoneyWithdrawnEvent(amount, balance - amount), this);
    }

    public void close() {
        if (balance != 0) {
            throw new IllegalStateException("Balance outstanding");
        }
        eventStream.append(new AccountClosedEvent(), this);
    }

    @Override
    public UUID id() {
        return accountId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public int balance() {
        return balance;
    }

    boolean isOpen() {
        return open;
    }

    void applySnapshot(AccountSnapshot snapshot) {
        this.ownerId = snapshot.getOwnerId();
        this.balance = snapshot.getBalance();
        this.open = snapshot.isOpen();
    }

    void apply(AccountOpenedEvent event) {
        this.ownerId = event.getOwnerId();
        this.balance = 0;
        this.open = true;
    }

    void apply(MoneyDepositedEvent event) {
        this.balance = event.getBalance();
    }

    void apply(MoneyWithdrawnEvent event) {
        this.balance = event.getBalance();
    }

    void apply(AccountClosedEvent event) {
        this.open = false;
    }

    private void requireOpenAccount() {
        if (!open) {
            throw new IllegalStateException("Account not open");
        }
    }

}
