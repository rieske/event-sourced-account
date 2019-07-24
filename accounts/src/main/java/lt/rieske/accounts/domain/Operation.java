package lt.rieske.accounts.domain;

import java.util.UUID;

public final class Operation {

    public static Transaction<Account> deposit(int amount, UUID transactionId) {
        return new Transaction<>(transactionId, a -> a.deposit(amount, transactionId));
    }

    public static Transaction<Account> withdraw(int amount, UUID transactionId) {
        return new Transaction<>(transactionId, a -> a.withdraw(amount, transactionId));
    }

    public static BiTransaction<Account> transfer(int amount, UUID transactionId) {
        return new BiTransaction<>(transactionId, (source, target) -> {
            source.withdraw(amount, transactionId);
            target.deposit(amount, transactionId);
        });
    }
}

