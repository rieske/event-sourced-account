package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.api.ApiConfiguration;
import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.domain.AccountEvent;
import lt.rieske.accounts.domain.AtomicOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class IdempotencyTest {

    private AggregateRepository<Account, AccountEvent> accountRepository;

    protected abstract EventStore<AccountEvent> getEventStore();

    private final UUID ownerId = UUID.randomUUID();

    @BeforeEach
    void init() {
        accountRepository = ApiConfiguration.accountRepository(getEventStore());
    }

    @Test
    void depositTransactionShouldBeIdempotent() {
        var accountId = UUID.randomUUID();
        accountRepository.create(accountId, UUID.randomUUID(), AtomicOperation.open(ownerId));

        var transactionId = UUID.randomUUID();
        accountRepository.transact(accountId, transactionId, AtomicOperation.deposit(10));
        accountRepository.transact(accountId, transactionId, AtomicOperation.deposit(10));

        var account = accountRepository.query(accountId);
        assertThat(account.balance()).isEqualTo(10);
    }

    @Test
    void withdrawalTransactionShouldBeIdempotent() {
        var accountId = UUID.randomUUID();
        accountRepository.create(accountId, UUID.randomUUID(), AtomicOperation.open(ownerId));
        accountRepository.transact(accountId, UUID.randomUUID(), AtomicOperation.deposit(100));

        var transactionId = UUID.randomUUID();
        accountRepository.transact(accountId, transactionId, AtomicOperation.withdraw(10));
        accountRepository.transact(accountId, transactionId, AtomicOperation.withdraw(10));

        var account = accountRepository.query(accountId);
        assertThat(account.balance()).isEqualTo(90);
    }

    @Test
    void moneyTransferTransactionShouldBeIdempotent() {
        var sourceAccountId = UUID.randomUUID();
        accountRepository.create(sourceAccountId, UUID.randomUUID(), AtomicOperation.open(ownerId));
        accountRepository.transact(sourceAccountId, UUID.randomUUID(), AtomicOperation.deposit(100));

        var targetAccountId = UUID.randomUUID();
        accountRepository.create(targetAccountId, UUID.randomUUID(), AtomicOperation.open(ownerId));

        var transactionId = UUID.randomUUID();
        accountRepository.transact(sourceAccountId, targetAccountId, transactionId, AtomicOperation.transfer(60));
        accountRepository.transact(sourceAccountId, targetAccountId, transactionId, AtomicOperation.transfer(60));

        var sourceAccount = accountRepository.query(sourceAccountId);
        assertThat(sourceAccount.balance()).isEqualTo(40);

        var targetAccount = accountRepository.query(targetAccountId);
        assertThat(targetAccount.balance()).isEqualTo(60);
    }
}
