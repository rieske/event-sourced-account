package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.api.ApiConfiguration;
import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.domain.AccountEventsVisitor;
import lt.rieske.accounts.domain.Operation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class MoneyTransferTest {

    private AggregateRepository<Account, AccountEventsVisitor> accountRepository;

    protected abstract EventStore<AccountEventsVisitor> getEventStore();

    private final UUID ownerId = UUID.randomUUID();
    private final UUID sourceAccountId = UUID.randomUUID();
    private final UUID targetAccountId = UUID.randomUUID();

    @BeforeEach
    void init() {
        accountRepository = ApiConfiguration.accountRepository(getEventStore());

        accountRepository.create(sourceAccountId, UUID.randomUUID(), Operation.open(ownerId));
        accountRepository.create(targetAccountId, UUID.randomUUID(), Operation.open(ownerId));
    }

    @Test
    void shouldTransferMoneyBetweenAccounts() {
        int transferAmount = 42;
        accountRepository.transact(sourceAccountId, UUID.randomUUID(), Operation.deposit(transferAmount));
        accountRepository.transact(sourceAccountId, targetAccountId, UUID.randomUUID(), Operation.transfer(transferAmount));

        var sourceAccount = accountRepository.query(sourceAccountId);
        assertThat(sourceAccount.balance()).isZero();

        var targetAccount = accountRepository.query(targetAccountId);
        assertThat(targetAccount.balance()).isEqualTo(transferAmount);
    }

    @Test
    void shouldFailMoneyTransferWhenSourceHasInsufficientBalance() {
        int transferAmount = 42;
        accountRepository.transact(sourceAccountId, UUID.randomUUID(), Operation.deposit(10));

        assertThatThrownBy(() -> accountRepository.transact(sourceAccountId, targetAccountId, UUID.randomUUID(),
                Operation.transfer(transferAmount)))
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
        accountRepository.transact(sourceAccountId, UUID.randomUUID(), Operation.deposit(transferAmount));
        accountRepository.transact(targetAccountId, UUID.randomUUID(), Operation.close());

        assertThatThrownBy(() -> accountRepository.transact(sourceAccountId, targetAccountId, UUID.randomUUID(),
                Operation.transfer(transferAmount)))
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
        accountRepository.transact(sourceAccountId, UUID.randomUUID(), Operation.close());

        assertThatThrownBy(() -> accountRepository.transact(sourceAccountId, targetAccountId, UUID.randomUUID(),
                Operation.transfer(transferAmount)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Account not open");

        var sourceAccount = accountRepository.query(sourceAccountId);
        assertThat(sourceAccount.balance()).isZero();

        var targetAccount = accountRepository.query(targetAccountId);
        assertThat(targetAccount.balance()).isZero();
    }

    @Test
    void shouldNotTransferNegativeAmount() {
        accountRepository.transact(sourceAccountId, UUID.randomUUID(), Operation.deposit(42));

        assertThatThrownBy(() -> accountRepository.transact(sourceAccountId, targetAccountId, UUID.randomUUID(),
                Operation.transfer(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Can not withdraw negative amount");

        var sourceAccount = accountRepository.query(sourceAccountId);
        assertThat(sourceAccount.balance()).isEqualTo(42);

        var targetAccount = accountRepository.query(targetAccountId);
        assertThat(targetAccount.balance()).isZero();
    }
}
