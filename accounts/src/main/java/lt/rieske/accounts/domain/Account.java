package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.Aggregate;
import lt.rieske.accounts.eventsourcing.EventStream;

import java.util.UUID;


public class Account implements Aggregate {
    private final EventStream<Account> eventStream;

    private final UUID accountId;

    private UUID ownerId;
    private long balance;
    private boolean open;

    public Account(EventStream<Account> eventStream, UUID accountId) {
        this.eventStream = eventStream;
        this.accountId = accountId;
    }

    public AccountSnapshot snapshot() {
        return new AccountSnapshot(accountId, ownerId, balance, open, null);
    }

    public void open(UUID ownerId, UUID transactionId) {
        if (this.ownerId != null) {
            throw new IllegalStateException("Account already has an owner");
        }
        eventStream.append(new AccountOpenedEvent(ownerId, transactionId), this);
    }

    public void deposit(long amount, UUID transactionId) {
        if (amount == 0) {
            return;
        }
        requireOpenAccount();
        if (amount < 0) {
            throw new IllegalArgumentException("Can not deposit negative amount: " + amount);
        }
        eventStream.append(new MoneyDepositedEvent(amount, balance + amount, transactionId), this);
    }

    public void withdraw(long amount, UUID transactionId) {
        if (amount == 0) {
            return;
        }
        requireOpenAccount();
        if (amount < 0) {
            throw new IllegalArgumentException("Can not withdraw negative amount: " + amount);
        }
        if (balance < amount) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        eventStream.append(new MoneyWithdrawnEvent(amount, balance - amount, transactionId), this);
    }

    public void close(UUID transactionId) {
        if (balance != 0) {
            throw new IllegalStateException("Balance outstanding");
        }
        eventStream.append(new AccountClosedEvent(transactionId), this);
    }

    @Override
    public UUID id() {
        return accountId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public long balance() {
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
