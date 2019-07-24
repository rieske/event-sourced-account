package lt.rieske.accounts.eventsourcing;

import java.util.UUID;
import java.util.function.BiConsumer;

public class BiTransaction<T> implements BiConsumer<T, T> {

    private final UUID transactionId;
    private final BiConsumer<T, T> consumer;

    public final UUID transactionId() {
        return transactionId;
    }

    public BiTransaction(UUID transactionId, BiConsumer<T, T> consumer) {
        this.transactionId = transactionId;
        this.consumer = consumer;
    }

    @Override
    public void accept(T t1, T t2) {
        consumer.accept(t1, t2);
    }
}
