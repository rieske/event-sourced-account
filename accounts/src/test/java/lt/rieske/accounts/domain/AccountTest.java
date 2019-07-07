package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.AggregateNotFoundException;
import lt.rieske.accounts.eventsourcing.Event;
import lt.rieske.accounts.eventsourcing.InMemoryEventStore;
import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AccountTest {

    private final InMemoryEventStore<Account> eventStore = new InMemoryEventStore<>();
    private final AccountRepository accountRepository = new AccountRepository(eventStore);
    private final SnapshottingAccountRepository snapshottingAccountRepository = new SnapshottingAccountRepository(eventStore);

    @SafeVarargs
    private void givenEvents(Event<Account>... events) {
        for (int i = 0; i < events.length; i++) {
            eventStore.append(events[i], i + 1);
        }
    }

    @Test
    public void shouldOpenAnAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        var account = accountRepository.create(accountId);
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
        givenEvents(new AccountOpenedEvent(accountId, ownerId));

        var account = accountRepository.load(accountId);

        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
    }

    @Test
    public void shouldLoadTwoDistinctAccounts() {
        var account1Id = UUID.randomUUID();
        var account2Id = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(new AccountOpenedEvent(account1Id, ownerId));
        givenEvents(new AccountOpenedEvent(account2Id, ownerId));

        var account1 = accountRepository.load(account1Id);
        var account2 = accountRepository.load(account2Id);

        assertThat(account1.id()).isEqualTo(account1Id);
        assertThat(account1.ownerId()).isEqualTo(ownerId);

        assertThat(account2.id()).isEqualTo(account2Id);
        assertThat(account2.ownerId()).isEqualTo(ownerId);
    }

    @Test
    public void shouldThrowWhenAccountIsNotFound() {
        assertThatThrownBy(() -> accountRepository.load(UUID.randomUUID()))
                .isInstanceOf(AggregateNotFoundException.class);
    }

    @Test
    public void shouldDepositMoneyToAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(new AccountOpenedEvent(accountId, ownerId));

        var account = accountRepository.load(accountId);
        account.deposit(42);

        assertThat(account.balance()).isEqualTo(42);
        assertThat(eventStore.getEvents(accountId)).containsExactly(
                new AccountOpenedEvent(accountId, ownerId),
                new MoneyDepositedEvent(accountId, 42, 42)
        );
    }

    @Test
    public void multipleDepositsShouldAccumulateBalance() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(new AccountOpenedEvent(accountId, ownerId));

        var account = accountRepository.load(accountId);
        account.deposit(1);
        account.deposit(1);

        assertThat(account.balance()).isEqualTo(2);
        assertThat(eventStore.getEvents(accountId)).containsExactly(
                new AccountOpenedEvent(accountId, ownerId),
                new MoneyDepositedEvent(accountId, 1, 1),
                new MoneyDepositedEvent(accountId, 1, 2)
        );
    }

    @Test
    public void shouldNotDepositZeroToAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(new AccountOpenedEvent(accountId, ownerId));

        var account = accountRepository.load(accountId);
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
        givenEvents(new AccountOpenedEvent(accountId, ownerId));

        var account = accountRepository.load(accountId);
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
        givenEvents(
                new AccountOpenedEvent(accountId, ownerId),
                new MoneyDepositedEvent(accountId, 10, 10)
        );

        var account = accountRepository.load(accountId);
        account.withdraw(5);

        assertThat(account.balance()).isEqualTo(5);
        assertThat(eventStore.getEvents(accountId)).containsExactly(
                new AccountOpenedEvent(accountId, ownerId),
                new MoneyDepositedEvent(accountId, 10, 10),
                new MoneyWithdrawnEvent(accountId, 5, 5)
        );
    }

    @Test
    public void shouldNotWithdrawZero() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(new AccountOpenedEvent(accountId, ownerId));

        var account = accountRepository.load(accountId);
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
        givenEvents(
                new AccountOpenedEvent(accountId, ownerId),
                new MoneyDepositedEvent(accountId, 10, 10)
        );

        var account = accountRepository.load(accountId);
        assertThatThrownBy(() -> account.withdraw(11)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    public void shouldInstantiateAccountFromSnapshot() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        eventStore.storeSnapshot(new AccountSnapshot(10, accountId, ownerId, 42));

        var account = snapshottingAccountRepository.load(accountId);
        assertThat(account.balance()).isEqualTo(42);
        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
    }

    @Test
    public void shouldNotReplayEventsPriorToSnapshot() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        eventStore.append(new MoneyDepositedEvent(accountId, 10, 11), 10);
        eventStore.storeSnapshot(new AccountSnapshot(10, accountId, ownerId, 42));

        var account = snapshottingAccountRepository.load(accountId);
        assertThat(account.balance()).isEqualTo(42);
        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
    }

    @Test
    public void shouldReplayEventsAfterSnapshot() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        eventStore.append(new MoneyDepositedEvent(accountId, 10, 11), 10);
        eventStore.append(new MoneyDepositedEvent(accountId, 1, 43), 11);
        eventStore.storeSnapshot(new AccountSnapshot(10, accountId, ownerId, 42));

        var account = snapshottingAccountRepository.load(accountId);
        assertThat(account.balance()).isEqualTo(43);
        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
    }

}
