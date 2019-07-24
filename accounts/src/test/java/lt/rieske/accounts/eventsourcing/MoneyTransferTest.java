package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.domain.Operation;
import lt.rieske.accounts.infrastructure.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class MoneyTransferTest {

    private AggregateRepository<Account> accountRepository;

    protected abstract EventStore<Account> getEventStore();

    private final UUID ownerId = UUID.randomUUID();
    private final UUID sourceAccountId = UUID.randomUUID();
    private final UUID targetAccountId = UUID.randomUUID();

    @BeforeEach
    void init() {
        accountRepository = Configuration.accountRepository(getEventStore());

        accountRepository.create(sourceAccountId, account -> account.open(ownerId));
        accountRepository.create(targetAccountId, account -> account.open(ownerId));
    }

    @Test
    void shouldTransferMoneyBetweenAccounts() {
        int transferAmount = 42;
        accountRepository.transact(sourceAccountId, Operation.deposit(transferAmount, UUID.randomUUID()));
        accountRepository.transact(sourceAccountId, targetAccountId, Operation.transfer(transferAmount, UUID.randomUUID()));

        var sourceAccount = accountRepository.query(sourceAccountId);
        assertThat(sourceAccount.balance()).isZero();

        var targetAccount = accountRepository.query(targetAccountId);
        assertThat(targetAccount.balance()).isEqualTo(transferAmount);
    }

    @Test
    void shouldFailMoneyTransferWhenSourceHasInsufficientBalance() {
        int transferAmount = 42;
        accountRepository.transact(sourceAccountId, Operation.deposit(10, UUID.randomUUID()));

        assertThatThrownBy(() -> accountRepository.transact(sourceAccountId, targetAccountId,
                Operation.transfer(transferAmount, UUID.randomUUID())))
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
        accountRepository.transact(sourceAccountId, Operation.deposit(transferAmount, UUID.randomUUID()));
        accountRepository.transact(targetAccountId, Account::close);

        assertThatThrownBy(() -> accountRepository.transact(sourceAccountId, targetAccountId,
                Operation.transfer(transferAmount, UUID.randomUUID())))
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
        accountRepository.transact(sourceAccountId, Account::close);

        assertThatThrownBy(() -> accountRepository.transact(sourceAccountId, targetAccountId,
                Operation.transfer(transferAmount, UUID.randomUUID())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Account not open");

        var sourceAccount = accountRepository.query(sourceAccountId);
        assertThat(sourceAccount.balance()).isZero();

        var targetAccount = accountRepository.query(targetAccountId);
        assertThat(targetAccount.balance()).isZero();
    }

    @Test
    void shouldNotTransferNegativeAmount() {
        accountRepository.transact(sourceAccountId, Operation.deposit(42, UUID.randomUUID()));

        assertThatThrownBy(() -> accountRepository.transact(sourceAccountId, targetAccountId,
                Operation.transfer(-1, UUID.randomUUID())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Can not withdraw negative amount");

        var sourceAccount = accountRepository.query(sourceAccountId);
        assertThat(sourceAccount.balance()).isEqualTo(42);

        var targetAccount = accountRepository.query(targetAccountId);
        assertThat(targetAccount.balance()).isZero();
    }
}
