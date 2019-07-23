package lt.rieske.accounts.domain;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class Operation {

    public static Consumer<Account> deposit(int amount, UUID transactionId) {
        return a -> a.deposit(amount, transactionId);
    }

    public static Consumer<Account> withdraw(int amount) {
        return a -> a.withdraw(amount);
    }

    public static BiConsumer<Account, Account> transfer(int amount) {
        return (source, target) -> {
            source.withdraw(amount);
            target.deposit(amount);
        };
    }
}
