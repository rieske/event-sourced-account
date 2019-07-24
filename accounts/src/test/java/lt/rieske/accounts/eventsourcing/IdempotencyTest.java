package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.domain.Operation;
import lt.rieske.accounts.infrastructure.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class IdempotencyTest {

    private AggregateRepository<Account> accountRepository;

    protected abstract EventStore<Account> getEventStore();

    private final UUID ownerId = UUID.randomUUID();

    @BeforeEach
    void init() {
        accountRepository = Configuration.accountRepository(getEventStore());
    }

    @Test
    void depositTransactionShouldBeIdempotent() {
        var accountId = UUID.randomUUID();
        accountRepository.create(accountId, Operation.open(ownerId));

        var transactionId = UUID.randomUUID();
        accountRepository.transact(accountId, Operation.deposit(10, transactionId));
        accountRepository.transact(accountId, Operation.deposit(10, transactionId));

        var account = accountRepository.query(accountId);
        assertThat(account.balance()).isEqualTo(10);
    }

    @Test
    void withdrawalTransactionShouldBeIdempotent() {
        var accountId = UUID.randomUUID();
        accountRepository.create(accountId, Operation.open(ownerId));
        accountRepository.transact(accountId, Operation.deposit(100, UUID.randomUUID()));

        var transactionId = UUID.randomUUID();
        accountRepository.transact(accountId, Operation.withdraw(10, transactionId));
        accountRepository.transact(accountId, Operation.withdraw(10, transactionId));

        var account = accountRepository.query(accountId);
        assertThat(account.balance()).isEqualTo(90);
    }

    @Test
    void moneyTransferTransactionShouldBeIdempotent() {
        var sourceAccountId = UUID.randomUUID();
        accountRepository.create(sourceAccountId, Operation.open(ownerId));
        accountRepository.transact(sourceAccountId, Operation.deposit(100, UUID.randomUUID()));

        var targetAccountId = UUID.randomUUID();
        accountRepository.create(targetAccountId, Operation.open(ownerId));

        var transactionId = UUID.randomUUID();
        accountRepository.transact(sourceAccountId, targetAccountId, Operation.transfer(60, transactionId));
        accountRepository.transact(sourceAccountId, targetAccountId, Operation.transfer(60, transactionId));

        var sourceAccount = accountRepository.query(sourceAccountId);
        assertThat(sourceAccount.balance()).isEqualTo(40);

        var targetAccount = accountRepository.query(targetAccountId);
        assertThat(targetAccount.balance()).isEqualTo(60);
    }
}
