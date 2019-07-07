package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.AggregateNotFoundException;
import lt.rieske.accounts.eventsourcing.InMemoryEventStore;
import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AccountTest {

    private final InMemoryEventStore<Account> eventStore = new InMemoryEventStore<>();
    private final AccountRepository accountRepository = new AccountRepository(eventStore);

    @Test
    public void shouldOpenAnAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        var account = accountRepository.newAccount(accountId);
        account.open(accountId, ownerId);

        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
        assertThat(account.balance()).isZero();
        assertThat(eventStore.getEvents(accountId)).containsExactly(new AccountOpenedEvent(accountId, ownerId));
    }

    @Test
    public void shouldLoadAnAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        eventStore.append(new AccountOpenedEvent(accountId, ownerId), 1);

        var account = accountRepository.loadAccount(accountId);

        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
    }

    @Test
    public void shouldLoadTwoDistinctAccounts() {
        var account1Id = UUID.randomUUID();
        var account2Id = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        eventStore.append(new AccountOpenedEvent(account1Id, ownerId), 1);
        eventStore.append(new AccountOpenedEvent(account2Id, ownerId), 2);

        var account1 = accountRepository.loadAccount(account1Id);
        var account2 = accountRepository.loadAccount(account2Id);

        assertThat(account1.id()).isEqualTo(account1Id);
        assertThat(account1.ownerId()).isEqualTo(ownerId);

        assertThat(account2.id()).isEqualTo(account2Id);
        assertThat(account2.ownerId()).isEqualTo(ownerId);
    }

    @Test
    public void shouldThrowWhenAccountIsNotFound() {
        assertThatThrownBy(() -> accountRepository.loadAccount(UUID.randomUUID()))
                .isInstanceOf(AggregateNotFoundException.class);
    }

    @Test
    public void shouldDepositMoneyToAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        eventStore.append(new AccountOpenedEvent(accountId, ownerId), 1);

        var account = accountRepository.loadAccount(accountId);
        account.deposit(42);

        assertThat(account.balance()).isEqualTo(42);
        assertThat(eventStore.getEvents(accountId)).containsExactly(
                new AccountOpenedEvent(accountId, ownerId),
                new MoneyDepositedEvent(accountId, 42)
        );
    }

    @Test
    public void multipleDepositsShouldAccumulateBalance() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        eventStore.append(new AccountOpenedEvent(accountId, ownerId), 1);

        var account = accountRepository.loadAccount(accountId);
        account.deposit(1);
        account.deposit(1);

        assertThat(account.balance()).isEqualTo(2);
        assertThat(eventStore.getEvents(accountId)).containsExactly(
                new AccountOpenedEvent(accountId, ownerId),
                new MoneyDepositedEvent(accountId, 1),
                new MoneyDepositedEvent(accountId, 1)
        );
    }

    @Test
    public void shouldNotDepositZeroToAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        eventStore.append(new AccountOpenedEvent(accountId, ownerId), 1);

        var account = accountRepository.loadAccount(accountId);
        account.deposit(0);

        assertThat(account.balance()).isZero();
        assertThat(eventStore.getEvents(accountId)).containsExactly(
                new AccountOpenedEvent(accountId, ownerId)
        );
    }

    @Test
    public void shouldThrowWhenDepositingNegativeAmount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        eventStore.append(new AccountOpenedEvent(accountId, ownerId), 1);

        var account = accountRepository.loadAccount(accountId);
        assertThatThrownBy(() -> account.deposit(-42)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Can not deposit negative amount");
        assertThat(eventStore.getEvents(accountId)).containsExactly(
                new AccountOpenedEvent(accountId, ownerId)
        );
    }

    @Test
    public void shouldWithdrawMoney() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        eventStore.append(new AccountOpenedEvent(accountId, ownerId),1 );
        eventStore.append(new MoneyDepositedEvent(accountId, 10), 2);

        var account = accountRepository.loadAccount(accountId);
        account.withdraw(5);

        assertThat(account.balance()).isEqualTo(5);
        assertThat(eventStore.getEvents(accountId)).containsExactly(
                new AccountOpenedEvent(accountId, ownerId),
                new MoneyDepositedEvent(accountId, 10),
                new MoneyWithdrawnEvent(accountId, 5)
        );
    }

    @Test
    public void shouldNotWithdrawZero() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        eventStore.append(new AccountOpenedEvent(accountId, ownerId), 1);

        var account = accountRepository.loadAccount(accountId);
        account.withdraw(0);

        assertThat(account.balance()).isEqualTo(0);
        assertThat(eventStore.getEvents(accountId)).containsExactly(
                new AccountOpenedEvent(accountId, ownerId)
        );
    }

    @Test
    public void shouldNotWithdrawMoneyWhenBalanceInsufficient() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        eventStore.append(new AccountOpenedEvent(accountId, ownerId), 1);
        eventStore.append(new MoneyDepositedEvent(accountId, 10), 2);

        var account = accountRepository.loadAccount(accountId);
        assertThatThrownBy(() -> account.withdraw(11)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance");
    }
}
