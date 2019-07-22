package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.domain.Operation;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class MoneyTransferTest {

    private AggregateRepository<Account> accountRepository;

    protected abstract EventStore<Account> getEventStore();

    private final UUID ownerId = UUID.randomUUID();
    private final UUID sourceAccountId = UUID.randomUUID();
    private final UUID targetAccountId = UUID.randomUUID();

    @Before
    public void init() {
        accountRepository = new AggregateRepository<>(getEventStore(), Account::new);

        accountRepository.create(sourceAccountId, account -> account.open(ownerId));
        accountRepository.create(targetAccountId, account -> account.open(ownerId));
    }

    @Test
    public void shouldTransferMoneyBetweenAccounts() {
        int transferAmount = 42;
        accountRepository.transact(sourceAccountId, Operation.deposit(transferAmount));
        accountRepository.transact(sourceAccountId, targetAccountId, Operation.transfer(transferAmount));

        var sourceAccount = accountRepository.query(sourceAccountId);
        assertThat(sourceAccount.balance()).isZero();

        var targetAccount = accountRepository.query(targetAccountId);
        assertThat(targetAccount.balance()).isEqualTo(transferAmount);
    }

    @Test
    public void shouldFailMoneyTransferWhenSourceHasInsufficientBalance() {
        int transferAmount = 42;
        accountRepository.transact(sourceAccountId, Operation.deposit(10));

        assertThatThrownBy(() -> accountRepository.transact(sourceAccountId, targetAccountId, Operation.transfer(transferAmount)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance");

        var sourceAccount = accountRepository.query(sourceAccountId);
        assertThat(sourceAccount.balance()).isEqualTo(10);

        var targetAccount = accountRepository.query(targetAccountId);
        assertThat(targetAccount.balance()).isZero();
    }

    @Test
    public void shouldFailMoneyTransferWhenTargetAccountIsClosed() {
        int transferAmount = 42;
        accountRepository.transact(sourceAccountId, Operation.deposit(transferAmount));
        accountRepository.transact(targetAccountId, Account::close);

        assertThatThrownBy(() -> accountRepository.transact(sourceAccountId, targetAccountId, Operation.transfer(transferAmount)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Account not open");

        var sourceAccount = accountRepository.query(sourceAccountId);
        assertThat(sourceAccount.balance()).isEqualTo(transferAmount);

        var targetAccount = accountRepository.query(targetAccountId);
        assertThat(targetAccount.balance()).isZero();
    }

    @Test
    public void shouldFailMoneyTransferWhenSourceAccountIsClosed() {
        int transferAmount = 42;
        accountRepository.transact(sourceAccountId, Account::close);

        assertThatThrownBy(() -> accountRepository.transact(sourceAccountId, targetAccountId, Operation.transfer(transferAmount)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Account not open");

        var sourceAccount = accountRepository.query(sourceAccountId);
        assertThat(sourceAccount.balance()).isZero();

        var targetAccount = accountRepository.query(targetAccountId);
        assertThat(targetAccount.balance()).isZero();
    }

    @Test
    public void shouldNotTransferNegativeAmount() {
        accountRepository.transact(sourceAccountId, Operation.deposit(42));

        assertThatThrownBy(() -> accountRepository.transact(sourceAccountId, targetAccountId, Operation.transfer(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Can not withdraw negative amount");

        var sourceAccount = accountRepository.query(sourceAccountId);
        assertThat(sourceAccount.balance()).isEqualTo(42);

        var targetAccount = accountRepository.query(targetAccountId);
        assertThat(targetAccount.balance()).isZero();
    }
}
