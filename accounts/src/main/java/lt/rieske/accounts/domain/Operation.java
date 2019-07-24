package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.BiTransaction;
import lt.rieske.accounts.eventsourcing.Transaction;

import java.util.UUID;

public final class Operation {

    public static Transaction<Account> open(UUID ownerId, UUID transactionId) {
        return new Transaction<>(transactionId, a -> a.open(ownerId, transactionId));
    }

    public static Transaction<Account> deposit(long amount, UUID transactionId) {
        return new Transaction<>(transactionId, a -> a.deposit(amount, transactionId));
    }

    public static Transaction<Account> withdraw(long amount, UUID transactionId) {
        return new Transaction<>(transactionId, a -> a.withdraw(amount, transactionId));
    }

    public static BiTransaction<Account> transfer(long amount, UUID transactionId) {
        return new BiTransaction<>(transactionId, (source, target) -> {
            source.withdraw(amount, transactionId);
            target.deposit(amount, transactionId);
        });
    }

    public static Transaction<Account> close(UUID transactionId) {
        return new Transaction<>(transactionId, a -> a.close(transactionId));
    }
}

