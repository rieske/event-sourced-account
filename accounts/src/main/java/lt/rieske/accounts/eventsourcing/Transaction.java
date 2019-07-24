package lt.rieske.accounts.eventsourcing;

import java.util.UUID;
import java.util.function.Consumer;

public class Transaction<T> implements Consumer<T> {

    private final UUID transactionId;
    private final Consumer<T> consumer;

    public final UUID transactionId() {
        return transactionId;
    }

    public Transaction(UUID transactionId, Consumer<T> consumer) {
        this.transactionId = transactionId;
        this.consumer = consumer;
    }

    @Override
    public void accept(T t) {
        consumer.accept(t);
    }
}
