package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.EventStream;
import lt.rieske.accounts.eventsourcing.EventVisitor;

import java.util.UUID;


public class Account implements EventVisitor<AccountEvent> {
    private final EventStream<Account, AccountEvent> eventStream;

    private final UUID accountId;

    private UUID ownerId;
    private long balance;
    private boolean open;

    public Account(EventStream<Account, AccountEvent> eventStream, UUID accountId) {
        this.eventStream = eventStream;
        this.accountId = accountId;
    }

    public AccountSnapshot snapshot() {
        return new AccountSnapshot(accountId, ownerId, balance, open);
    }

    public void open(UUID ownerId) {
        if (this.ownerId != null) {
            throw new IllegalStateException("Account already has an owner");
        }
        eventStream.append(new AccountOpenedEvent(ownerId), this, accountId);
    }

    public void deposit(long amount) {
        if (amount == 0) {
            return;
        }
        requireOpenAccount();
        if (amount < 0) {
            throw new IllegalArgumentException("Can not deposit negative amount: " + amount);
        }
        eventStream.append(new MoneyDepositedEvent(amount, balance + amount), this, accountId);
    }

    public void withdraw(long amount) {
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
        eventStream.append(new MoneyWithdrawnEvent(amount, balance - amount), this, accountId);
    }

    public void close() {
        if (balance != 0) {
            throw new IllegalStateException("Balance outstanding");
        }
        eventStream.append(new AccountClosedEvent(), this, accountId);
    }

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

    private void requireOpenAccount() {
        if (!open) {
            throw new IllegalStateException("Account not open");
        }
    }

    @Override
    public void visit(AccountEvent event) {
        switch (event) {
            case AccountSnapshot snapshot -> {
                this.ownerId = snapshot.ownerId();
                this.balance = snapshot.balance();
                this.open = snapshot.open();
            }
            case AccountOpenedEvent accountOpened -> {
                this.ownerId = accountOpened.ownerId();
                this.balance = 0;
                this.open = true;
            }
            case AccountClosedEvent accountClosed -> this.open = false;
            case MoneyDepositedEvent moneyDeposited -> this.balance = moneyDeposited.balance();
            case MoneyWithdrawnEvent moneyWithdrawn -> this.balance = moneyWithdrawn.balance();
        }
    }
}
