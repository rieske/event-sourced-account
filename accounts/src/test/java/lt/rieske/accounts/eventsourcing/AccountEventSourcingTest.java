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
import java.util.ConcurrentModificationException;
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
            sequencedEvents.add(new SequencedEvent<>(accountId, i + 1, events[i]));
        }
        eventStore.append(sequencedEvents, null);
    }

    @Test
    public void shouldOpenAnAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        accountRepository.create(accountId, account -> account.open(ownerId));
        var account = accountRepository.query(accountId);

        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
        assertThat(account.balance()).isZero();
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new SequencedEvent<>(accountId, 1, new AccountOpenedEvent(ownerId)));
    }

    @Test
    public void shouldConflictIfAccountWithIdExists() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        accountRepository.create(accountId, account -> account.open(ownerId));

        assertThatThrownBy(() -> accountRepository.create(accountId, account -> account.open(UUID.randomUUID())))
                .isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    public void shouldLoadAnAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId, new AccountOpenedEvent(ownerId));

        var account = accountRepository.query(accountId);

        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
    }

    @Test
    public void shouldLoadTwoDistinctAccounts() {
        var account1Id = UUID.randomUUID();
        var account2Id = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(account1Id, new AccountOpenedEvent(ownerId));
        givenEvents(account2Id, new AccountOpenedEvent(ownerId));

        var account1 = accountRepository.query(account1Id);
        var account2 = accountRepository.query(account2Id);

        assertThat(account1.id()).isEqualTo(account1Id);
        assertThat(account1.ownerId()).isEqualTo(ownerId);

        assertThat(account2.id()).isEqualTo(account2Id);
        assertThat(account2.ownerId()).isEqualTo(ownerId);
    }

    @Test
    public void shouldThrowWhenAccountIsNotFound() {
        assertThatThrownBy(() -> accountRepository.query(UUID.randomUUID()))
                .isInstanceOf(AggregateNotFoundException.class);
    }

    @Test
    public void shouldDepositMoneyToAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId, new AccountOpenedEvent(ownerId));

        accountRepository.transact(accountId, account -> account.deposit(42));

        var account = accountRepository.query(accountId);
        assertThat(account.balance()).isEqualTo(42);
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new SequencedEvent<>(accountId, 1, new AccountOpenedEvent(ownerId)),
                new SequencedEvent<>(accountId, 2, new MoneyDepositedEvent(42, 42))
        );
    }

    @Test
    public void shouldConflictOnConcurrentModification() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId, new AccountOpenedEvent(ownerId));

        var eventStream1 = new TransactionalEventStream<>(eventStore, (aggregate, version) -> null);
        var account1 = new Account(eventStream1, accountId);
        eventStream1.replay(account1);
        account1.deposit(42);

        var eventStream2 = new TransactionalEventStream<>(eventStore, (aggregate, version) -> null);
        var account2 = new Account(eventStream2, accountId);
        eventStream2.replay(account2);
        account2.deposit(42);
        eventStream2.commit();

        assertThatThrownBy(eventStream1::commit).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    public void multipleDepositsShouldAccumulateBalance() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId, new AccountOpenedEvent(ownerId));

        accountRepository.transact(accountId, account -> {
            account.deposit(1);
            account.deposit(1);
        });

        var account = accountRepository.query(accountId);
        assertThat(account.balance()).isEqualTo(2);
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new SequencedEvent<>(accountId, 1, new AccountOpenedEvent(ownerId)),
                new SequencedEvent<>(accountId, 2, new MoneyDepositedEvent(1, 1)),
                new SequencedEvent<>(accountId, 3, new MoneyDepositedEvent(1, 2))
        );
    }

    @Test
    public void shouldNotDepositZeroToAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId, new AccountOpenedEvent(ownerId));

        var account = accountRepository.query(accountId);
        account.deposit(0);

        assertThat(account.balance()).isZero();
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new SequencedEvent<>(accountId, 1, new AccountOpenedEvent(ownerId))
        );
    }

    @Test
    public void shouldThrowWhenDepositingNegativeAmount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId, new AccountOpenedEvent(ownerId));

        var account = accountRepository.query(accountId);
        assertThatThrownBy(() -> account.deposit(-42)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Can not deposit negative amount");
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new SequencedEvent<>(accountId, 1, new AccountOpenedEvent(ownerId))
        );
    }

    @Test
    public void shouldWithdrawMoney() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId,
                new AccountOpenedEvent(ownerId),
                new MoneyDepositedEvent(10, 10)
        );

        accountRepository.transact(accountId, account -> account.withdraw(5));

        var account = accountRepository.query(accountId);
        assertThat(account.balance()).isEqualTo(5);
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new SequencedEvent<>(accountId, 1, new AccountOpenedEvent(ownerId)),
                new SequencedEvent<>(accountId, 2, new MoneyDepositedEvent(10, 10)),
                new SequencedEvent<>(accountId, 3, new MoneyWithdrawnEvent(5, 5))
        );
    }

    @Test
    public void shouldNotWithdrawZero() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId, new AccountOpenedEvent(ownerId));

        var account = accountRepository.query(accountId);
        account.withdraw(0);

        assertThat(account.balance()).isEqualTo(0);
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new SequencedEvent<>(accountId, 1, new AccountOpenedEvent(ownerId))
        );
    }

    @Test
    public void shouldNotWithdrawMoneyWhenBalanceInsufficient() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId,
                new AccountOpenedEvent(ownerId),
                new MoneyDepositedEvent(10, 10)
        );

        var account = accountRepository.query(accountId);
        assertThatThrownBy(() -> account.withdraw(11)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    public void shouldCreateSnapshotAfterFiveEvents() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        givenEvents(accountId,
                new AccountOpenedEvent(ownerId),
                new MoneyDepositedEvent(10, 10)
        );
        snapshottingAccountRepository.transact(accountId, account -> {
            account.deposit(5);
            account.deposit(5);
            account.deposit(5);
        });

        var snapshot = eventStore.loadSnapshot(accountId);
        assertThat(snapshot)
                .isEqualTo(new SequencedEvent<>(accountId, 5, new AccountSnapshot(accountId, ownerId, 25, true)));

        snapshottingAccountRepository.transact(accountId, account -> {
            account.deposit(5);
            account.deposit(5);
            account.deposit(5);
            account.deposit(5);
            account.deposit(5);
        });

        snapshot = eventStore.loadSnapshot(accountId);
        assertThat(snapshot)
                .isEqualTo(new SequencedEvent<>(accountId, 10, new AccountSnapshot(accountId, ownerId, 50, true)));
    }

    @Test
    public void shouldInstantiateAccountFromSnapshot() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        eventStore.append(List.of(),
                new SequencedEvent<>(accountId, 10, new AccountSnapshot(accountId, ownerId, 42, true)));

        var account = snapshottingAccountRepository.query(accountId);
        assertThat(account.balance()).isEqualTo(42);
        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
    }

    @Test
    public void shouldNotReplayEventsPriorToSnapshot() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        eventStore.append(
                List.of(new SequencedEvent<>(accountId, 10, new MoneyDepositedEvent(10, 11))),
                new SequencedEvent<>(accountId, 10, new AccountSnapshot(accountId, ownerId, 42, true)));

        var account = snapshottingAccountRepository.query(accountId);
        assertThat(account.balance()).isEqualTo(42);
        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
    }

    @Test
    public void shouldReplayEventsAfterSnapshot() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        eventStore.append(
                List.of(new SequencedEvent<>(accountId, 10, new MoneyDepositedEvent(10, 11)),
                        new SequencedEvent<>(accountId, 11, new MoneyDepositedEvent(1, 43))),
                new SequencedEvent<>(accountId, 10, new AccountSnapshot(accountId, ownerId, 42, true)));

        var account = snapshottingAccountRepository.query(accountId);
        assertThat(account.balance()).isEqualTo(43);
        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
    }

}
