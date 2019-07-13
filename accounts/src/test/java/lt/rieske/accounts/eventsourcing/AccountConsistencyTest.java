package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.domain.AccountSnapshotter;
import org.junit.Before;
import org.junit.Test;

import java.util.ConcurrentModificationException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AccountConsistencyTest {

    private EventStore<Account> eventStore;
    private AggregateRepository<Account> accountRepository;
    private AggregateRepository<Account> snapshottingAccountRepository;

    private UUID accountId;

    protected abstract EventStore<Account> getEventStore();

    protected int depositCount() {
        return 100;
    }

    protected int threadCount() {
        return 8;
    }

    protected UUID aggregateId() {
        return accountId;
    }

    @Before
    public void init() {
        eventStore = getEventStore();
        accountRepository = new AggregateRepository<>(eventStore, Account::new);
        snapshottingAccountRepository = new AggregateRepository<>(eventStore, Account::new, new AccountSnapshotter(5));
    }

    @Test
    public void accountRemainsConsistentWithConcurrentModifications_noSnapshots() throws InterruptedException {
        accountRemainsConsistentWithConcurrentModifications(accountRepository);
    }

    @Test
    public void accountRemainsConsistentWithConcurrentModifications_withSnapshotting() throws InterruptedException {
        accountRemainsConsistentWithConcurrentModifications(snapshottingAccountRepository);
    }

    private void accountRemainsConsistentWithConcurrentModifications(AggregateRepository<Account> repository)
            throws InterruptedException {
        accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        repository.create(accountId, account -> account.open(accountId, ownerId));

        var depositCount = depositCount();
        var threadCount = threadCount();
        var executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < depositCount; i++) {
            var latch = new CountDownLatch(threadCount);
            for (int j = 0; j < threadCount; j++) {
                executor.submit(() -> {
                    while (true) {
                        try {
                            repository.transact(accountId, account -> account.deposit(1));
                            break;
                        } catch (RuntimeException e) {
                            if (!e.getClass().equals(ConcurrentModificationException.class)) {
                                e.printStackTrace();
                                break;
                            }
                        }
                    }
                    latch.countDown();
                });
            }
            latch.await();
        }

        assertThat(accountRepository.query(accountId).balance()).isEqualTo(depositCount * threadCount);
        assertThat(snapshottingAccountRepository.query(accountId).balance()).isEqualTo(depositCount * threadCount);
    }
}
