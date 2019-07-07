package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.InMemoryEventStore;
import org.junit.Test;

import java.util.ConcurrentModificationException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

public class AccountConsistencyTest {

    private final InMemoryEventStore<Account> eventStore = new InMemoryEventStore<>();
    private final AccountRepository accountRepository = new AccountRepository(eventStore);

    @Test
    public void accountRemainsConsistentWithConcurrentModifications() throws InterruptedException {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        var account = accountRepository.newAccount(accountId);
        account.open(accountId, ownerId);

        var depositCount = 100;
        var threadCount = 8;
        var executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < depositCount; i++) {
            var latch = new CountDownLatch(threadCount);
            for (int j = 0; j < threadCount; j++) {
                executor.submit(() -> {
                    while (true) {
                        try {
                            accountRepository.loadAccount(accountId).deposit(1);
                            break;
                        } catch (ConcurrentModificationException ignored) {
                            // load aggregate and retry
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                    latch.countDown();
                });
            }
            latch.await();
        }

        assertThat(accountRepository.loadAccount(accountId).balance()).isEqualTo(depositCount * threadCount);
        var events = eventStore.getSequencedEvents(accountId);
        for (int i = 0; i < depositCount * threadCount; i++) {
            assertThat(events.get(i).getSequenceNumber()).isEqualTo(i + 1);
        }
    }
}
