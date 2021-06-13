package lt.rieske.accounts.domain;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class Operation {

    public static Consumer<Account> open(UUID ownerId) {
        return a -> a.open(ownerId);
    }

    public static Consumer<Account> deposit(long amount) {
        return a -> a.deposit(amount);
    }

    public static Consumer<Account> withdraw(long amount) {
        return a -> a.withdraw(amount);
    }

    public static BiConsumer<Account, Account> transfer(long amount) {
        return (source, target) -> {
            source.withdraw(amount);
            target.deposit(amount);
        };
    }

    public static Consumer<Account> close() {
        return Account::close;
    }
}

