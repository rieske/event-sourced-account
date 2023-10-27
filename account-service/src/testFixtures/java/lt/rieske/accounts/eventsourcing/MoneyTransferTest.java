package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.api.ApiConfiguration;
import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.domain.AccountEvent;
import lt.rieske.accounts.domain.AtomicOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public abstract class MoneyTransferTest {

    private AggregateRepository<Account, AccountEvent> accountRepository;

    protected abstract EventStore<AccountEvent> getEventStore();

    private final UUID ownerId = UUID.randomUUID();
    private final UUID sourceAccountId = UUID.randomUUID();
    private final UUID targetAccountId = UUID.randomUUID();

    @BeforeEach
    void init() {
        accountRepository = ApiConfiguration.accountRepository(getEventStore());

        accountRepository.create(sourceAccountId, UUID.randomUUID(), AtomicOperation.open(ownerId));
        accountRepository.create(targetAccountId, UUID.randomUUID(), AtomicOperation.open(ownerId));
    }

    @Test
    void shouldTransferMoneyBetweenAccounts() {
        int transferAmount = 42;
        accountRepository.transact(sourceAccountId, UUID.randomUUID(), AtomicOperation.deposit(transferAmount));
        accountRepository.transact(sourceAccountId, targetAccountId, UUID.randomUUID(), AtomicOperation.transfer(transferAmount));

        var sourceAccount = accountRepository.query(sourceAccountId);
        assertThat(sourceAccount.balance()).isZero();

        var targetAccount = accountRepository.query(targetAccountId);
        assertThat(targetAccount.balance()).isEqualTo(transferAmount);
    }

    @Test
    void shouldFailMoneyTransferWhenSourceHasInsufficientBalance() {
        int transferAmount = 42;
        accountRepository.transact(sourceAccountId, UUID.randomUUID(), AtomicOperation.deposit(10));

        assertThatThrownBy(() -> accountRepository.transact(sourceAccountId, targetAccountId, UUID.randomUUID(),
                AtomicOperation.transfer(transferAmount)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance");

        var sourceAccount = accountRepository.query(sourceAccountId);
        assertThat(sourceAccount.balance()).isEqualTo(10);

        var targetAccount = accountRepository.query(targetAccountId);
        assertThat(targetAccount.balance()).isZero();
    }

    @Test
    void shouldFailMoneyTransferWhenTargetAccountIsClosed() {
        int transferAmount = 42;
        accountRepository.transact(sourceAccountId, UUID.randomUUID(), AtomicOperation.deposit(transferAmount));
        accountRepository.transact(targetAccountId, UUID.randomUUID(), AtomicOperation.close());

        assertThatThrownBy(() -> accountRepository.transact(sourceAccountId, targetAccountId, UUID.randomUUID(),
                AtomicOperation.transfer(transferAmount)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Account not open");

        var sourceAccount = accountRepository.query(sourceAccountId);
        assertThat(sourceAccount.balance()).isEqualTo(transferAmount);

        var targetAccount = accountRepository.query(targetAccountId);
        assertThat(targetAccount.balance()).isZero();
    }

    @Test
    void shouldFailMoneyTransferWhenSourceAccountIsClosed() {
        int transferAmount = 42;
        accountRepository.transact(sourceAccountId, UUID.randomUUID(), AtomicOperation.close());

        assertThatThrownBy(() -> accountRepository.transact(sourceAccountId, targetAccountId, UUID.randomUUID(),
                AtomicOperation.transfer(transferAmount)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Account not open");

        var sourceAccount = accountRepository.query(sourceAccountId);
        assertThat(sourceAccount.balance()).isZero();

        var targetAccount = accountRepository.query(targetAccountId);
        assertThat(targetAccount.balance()).isZero();
    }

    @Test
    void shouldNotTransferNegativeAmount() {
        accountRepository.transact(sourceAccountId, UUID.randomUUID(), AtomicOperation.deposit(42));

        assertThatThrownBy(() -> accountRepository.transact(sourceAccountId, targetAccountId, UUID.randomUUID(),
                AtomicOperation.transfer(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Can not withdraw negative amount");

        var sourceAccount = accountRepository.query(sourceAccountId);
        assertThat(sourceAccount.balance()).isEqualTo(42);

        var targetAccount = accountRepository.query(targetAccountId);
        assertThat(targetAccount.balance()).isZero();
    }
}
