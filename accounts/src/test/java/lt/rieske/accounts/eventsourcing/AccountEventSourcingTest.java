package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.domain.AccountOpenedEvent;
import lt.rieske.accounts.domain.AccountSnapshot;
import lt.rieske.accounts.domain.AccountSnapshotter;
import lt.rieske.accounts.domain.MoneyDepositedEvent;
import lt.rieske.accounts.domain.MoneyWithdrawnEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class AccountEventSourcingTest {

    private EventStore<Account> eventStore;
    private AggregateRepository<Account> accountRepository;
    private AggregateRepository<Account> snapshottingAccountRepository;

    protected abstract EventStore<Account> getEventStore();

    @Before
    public void init() {
        eventStore = getEventStore();
        accountRepository = new AggregateRepository<>(eventStore, Account::new);
        snapshottingAccountRepository = new AggregateRepository<>(eventStore, Account::new, new AccountSnapshotter(5));
    }

    @SafeVarargs
    private void givenEvents(UUID accountId, Event<Account>... events) {
        List<SequencedEvent<Account>> sequencedEvents = new ArrayList<>();
        for (int i = 0; i < events.length; i++) {
            sequencedEvents.add(new SequencedEvent<>(i + 1, events[i]));
        }
        eventStore.append(accountId, sequencedEvents, null);
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
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(new AccountOpenedEvent(accountId, ownerId));
    }

    @Test
    public void shouldLoadAnAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId, new AccountOpenedEvent(accountId, ownerId));

        var account = accountRepository.load(accountId);

        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
    }

    @Test
    public void shouldLoadTwoDistinctAccounts() {
        var account1Id = UUID.randomUUID();
        var account2Id = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(account1Id, new AccountOpenedEvent(account1Id, ownerId));
        givenEvents(account2Id, new AccountOpenedEvent(account2Id, ownerId));

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
        givenEvents(accountId, new AccountOpenedEvent(accountId, ownerId));

        var account = accountRepository.load(accountId);
        account.deposit(42);

        assertThat(account.balance()).isEqualTo(42);
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new AccountOpenedEvent(accountId, ownerId),
                new MoneyDepositedEvent(42, 42)
        );
    }

    @Test
    public void multipleDepositsShouldAccumulateBalance() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId, new AccountOpenedEvent(accountId, ownerId));

        var account = accountRepository.load(accountId);
        account.deposit(1);
        account.deposit(1);

        assertThat(account.balance()).isEqualTo(2);
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new AccountOpenedEvent(accountId, ownerId),
                new MoneyDepositedEvent(1, 1),
                new MoneyDepositedEvent(1, 2)
        );
    }

    @Test
    public void shouldNotDepositZeroToAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId, new AccountOpenedEvent(accountId, ownerId));

        var account = accountRepository.load(accountId);
        account.deposit(0);

        assertThat(account.balance()).isZero();
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new AccountOpenedEvent(accountId, ownerId)
        );
    }

    @Test
    public void shouldThrowWhenDepositingNegativeAmount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId, new AccountOpenedEvent(accountId, ownerId));

        var account = accountRepository.load(accountId);
        assertThatThrownBy(() -> account.deposit(-42)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Can not deposit negative amount");
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new AccountOpenedEvent(accountId, ownerId)
        );
    }

    @Test
    public void shouldWithdrawMoney() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId,
                new AccountOpenedEvent(accountId, ownerId),
                new MoneyDepositedEvent(10, 10)
        );

        var account = accountRepository.load(accountId);
        account.withdraw(5);

        assertThat(account.balance()).isEqualTo(5);
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new AccountOpenedEvent(accountId, ownerId),
                new MoneyDepositedEvent(10, 10),
                new MoneyWithdrawnEvent(5, 5)
        );
    }

    @Test
    public void shouldNotWithdrawZero() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId, new AccountOpenedEvent(accountId, ownerId));

        var account = accountRepository.load(accountId);
        account.withdraw(0);

        assertThat(account.balance()).isEqualTo(0);
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new AccountOpenedEvent(accountId, ownerId)
        );
    }

    @Test
    public void shouldNotWithdrawMoneyWhenBalanceInsufficient() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId,
                new AccountOpenedEvent(accountId, ownerId),
                new MoneyDepositedEvent(10, 10)
        );

        var account = accountRepository.load(accountId);
        assertThatThrownBy(() -> account.withdraw(11)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    public void shouldCreateSnapshotAfterFiveEvents() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        givenEvents(accountId,
                new AccountOpenedEvent(accountId, ownerId),
                new MoneyDepositedEvent(10, 10)
        );
        var account = snapshottingAccountRepository.load(accountId);
        account.deposit(5);
        account.deposit(5);
        account.deposit(5);

        var snapshot = eventStore.loadSnapshot(accountId);
        assertThat(snapshot).isEqualTo(new Snapshot<>(new AccountSnapshot(accountId, ownerId, 25, true), 5));

        account.deposit(5);
        account.deposit(5);
        account.deposit(5);
        account.deposit(5);
        account.deposit(5);

        snapshot = eventStore.loadSnapshot(accountId);
        assertThat(snapshot).isEqualTo(new Snapshot<>(new AccountSnapshot(accountId, ownerId, 50, true), 10));
    }

    @Test
    public void shouldInstantiateAccountFromSnapshot() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        eventStore.append(accountId, List.of(),
                new SequencedEvent<>(10, new AccountSnapshot(accountId, ownerId, 42, true)));

        var account = snapshottingAccountRepository.load(accountId);
        assertThat(account.balance()).isEqualTo(42);
        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
    }

    @Test
    public void shouldNotReplayEventsPriorToSnapshot() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        eventStore.append(accountId,
                List.of(new SequencedEvent<>(10, new MoneyDepositedEvent(10, 11))),
                new SequencedEvent<>(10, new AccountSnapshot(accountId, ownerId, 42, true)));

        var account = snapshottingAccountRepository.load(accountId);
        assertThat(account.balance()).isEqualTo(42);
        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
    }

    @Test
    public void shouldReplayEventsAfterSnapshot() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        eventStore.append(accountId,
                List.of(new SequencedEvent<>(10, new MoneyDepositedEvent(10, 11)),
                        new SequencedEvent<>(11, new MoneyDepositedEvent(1, 43))),
                new SequencedEvent<>(10, new AccountSnapshot(accountId, ownerId, 42, true)));

        var account = snapshottingAccountRepository.load(accountId);
        assertThat(account.balance()).isEqualTo(43);
        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
    }

}
