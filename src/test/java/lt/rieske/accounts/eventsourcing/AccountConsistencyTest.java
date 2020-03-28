package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.api.ApiConfiguration;
import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.domain.AccountEventsVisitor;
import lt.rieske.accounts.domain.Operation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;


abstract class AccountConsistencyTest {

    private AggregateRepository<Account, AccountEventsVisitor> accountRepository;
    private AggregateRepository<Account, AccountEventsVisitor> snapshottingAccountRepository;

    private ExecutorService executor;

    private final Set<UUID> accountIds = new HashSet<>();

    private final UUID ownerId = UUID.randomUUID();

    protected abstract EventStore<AccountEventsVisitor> getEventStore();

    protected int operationCount() {
        return 50;
    }

    protected int threadCount() {
        return 8;
    }

    Set<UUID> aggregateIds() {
        return accountIds;
    }

    @BeforeEach
    void init() {
        var eventStore = getEventStore();
        accountRepository = ApiConfiguration.accountRepository(eventStore);
        snapshottingAccountRepository = ApiConfiguration.snapshottingAccountRepository(eventStore, 5);
        executor = Executors.newFixedThreadPool(threadCount());
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    void accountsRemainConsistentWithConcurrentTransfers() throws InterruptedException {
        accountsRemainConsistentWithConcurrentTransfers(accountRepository);
    }

    @Test
    void accountsRemainConsistentWithConcurrentTransfers_withSnapshotting() throws InterruptedException {
        accountsRemainConsistentWithConcurrentTransfers(snapshottingAccountRepository);
    }

    @Test
    void accountRemainsConsistentWithConcurrentDeposits() throws InterruptedException {
        accountRemainsConsistentWithConcurrentDeposits(accountRepository);
    }

    @Test
    void accountRemainsConsistentWithConcurrentDeposits_withSnapshotting() throws InterruptedException {
        accountRemainsConsistentWithConcurrentDeposits(snapshottingAccountRepository);
    }

    @Test
    void accountRemainsConsistentWithConcurrentIdempotentDeposits() throws InterruptedException {
        accountRemainsConsistentWithConcurrentIdempotentDeposits(accountRepository);
    }

    @Test
    void accountRemainsConsistentWithConcurrentIdempotentDeposits_withSnapshotting() throws InterruptedException {
        accountRemainsConsistentWithConcurrentIdempotentDeposits(snapshottingAccountRepository);
    }

    void accountRemainsConsistentWithConcurrentDeposits(AggregateRepository<Account, AccountEventsVisitor> repository)
            throws InterruptedException {
        var accountId = openNewAccount(repository);

        var operationCount = operationCount();
        var threadCount = threadCount();

        for (int i = 0; i < operationCount; i++) {
            var latch = new CountDownLatch(threadCount);
            for (int j = 0; j < threadCount; j++) {
                executor.submit(() -> {
                    withRetryOnConcurrentModification(() ->
                            repository.transact(accountId, UUID.randomUUID(), Operation.deposit(1)));
                    latch.countDown();
                });
            }
            latch.await();
        }

        assertThat(accountRepository.query(accountId).balance()).isEqualTo(operationCount * threadCount);
        assertThat(snapshottingAccountRepository.query(accountId).balance()).isEqualTo(operationCount * threadCount);
    }

    private void accountRemainsConsistentWithConcurrentIdempotentDeposits(AggregateRepository<Account, AccountEventsVisitor> repository)
            throws InterruptedException {
        var accountId = openNewAccount(repository);

        var operationCount = operationCount();
        var threadCount = threadCount();

        for (int i = 0; i < operationCount; i++) {
            var latch = new CountDownLatch(threadCount);
            var transactionId = UUID.randomUUID();
            for (int j = 0; j < threadCount; j++) {
                executor.submit(() -> {
                    withRetryOnConcurrentModification(() ->
                            repository.transact(accountId, transactionId, Operation.deposit(1)));
                    latch.countDown();
                });
            }
            latch.await();
        }

        assertThat(accountRepository.query(accountId).balance()).isEqualTo(operationCount);
        assertThat(snapshottingAccountRepository.query(accountId).balance()).isEqualTo(operationCount);
    }

    private void accountsRemainConsistentWithConcurrentTransfers(AggregateRepository<Account, AccountEventsVisitor> repository)
            throws InterruptedException {
        var operationCount = operationCount();
        var threadCount = threadCount();

        var sourceAccountId = openNewAccount(repository);
        repository.transact(sourceAccountId, UUID.randomUUID(), Operation.deposit(operationCount));

        var targetAccountId = openNewAccount(repository);
        repository.transact(targetAccountId, UUID.randomUUID(), Operation.deposit(operationCount));

        for (int i = 0; i < operationCount; i++) {
            var latch = new CountDownLatch(threadCount);
            var transactionId = UUID.randomUUID();
            for (int j = 0; j < threadCount; j++) {
                executor.submit(() -> {
                    withRetryOnConcurrentModification(() ->
                            repository.transact(sourceAccountId, targetAccountId, transactionId, Operation.transfer(1)));
                    latch.countDown();
                });
            }
            latch.await();
        }

        assertThat(accountRepository.query(sourceAccountId).balance()).isZero();
        assertThat(snapshottingAccountRepository.query(targetAccountId).balance()).isEqualTo(operationCount * 2);
    }

    private UUID openNewAccount(AggregateRepository<Account, AccountEventsVisitor> repository) {
        var accountId = UUID.randomUUID();
        repository.create(accountId, UUID.randomUUID(), Operation.open(ownerId));
        accountIds.add(accountId);
        return accountId;
    }

    private void withRetryOnConcurrentModification(Runnable r) {
        while (true) {
            try {
                r.run();
                break;
            } catch (ConcurrentModificationException ignored) {
                // retry operation
            } catch (RuntimeException e) {
                e.printStackTrace();
                break;
            }
        }
    }
}
