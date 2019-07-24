package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.api.ApiConfiguration;
import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.domain.AccountOpenedEvent;
import lt.rieske.accounts.domain.AccountSnapshot;
import lt.rieske.accounts.domain.MoneyDepositedEvent;
import lt.rieske.accounts.domain.MoneyWithdrawnEvent;
import lt.rieske.accounts.domain.Operation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @BeforeEach
    void init() {
        eventStore = getEventStore();
        accountRepository = ApiConfiguration.accountRepository(eventStore);
        snapshottingAccountRepository = ApiConfiguration.snapshottingAccountRepository(eventStore, 5);
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
    void shouldOpenAnAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        accountRepository.create(accountId, Operation.open(ownerId));
        var account = accountRepository.query(accountId);

        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
        assertThat(account.balance()).isZero();
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new SequencedEvent<>(accountId, 1, new AccountOpenedEvent(ownerId)));
    }

    @Test
    void shouldConflictIfAccountWithIdExists() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        accountRepository.create(accountId, Operation.open(ownerId));

        assertThatThrownBy(() -> accountRepository.create(accountId, Operation.open(UUID.randomUUID())))
                .isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    void shouldLoadAnAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId, new AccountOpenedEvent(ownerId));

        var account = accountRepository.query(accountId);

        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
    }

    @Test
    void shouldLoadTwoDistinctAccounts() {
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
    void shouldThrowWhenAccountIsNotFound() {
        assertThatThrownBy(() -> accountRepository.query(UUID.randomUUID()))
                .isInstanceOf(AggregateNotFoundException.class);
    }

    @Test
    void shouldDepositMoneyToAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId, new AccountOpenedEvent(ownerId));

        var transactionId = UUID.randomUUID();
        accountRepository.transact(accountId, Operation.deposit(42, transactionId));

        var account = accountRepository.query(accountId);
        assertThat(account.balance()).isEqualTo(42);
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new SequencedEvent<>(accountId, 1, new AccountOpenedEvent(ownerId)),
                new SequencedEvent<>(accountId, 2, new MoneyDepositedEvent(42, 42, transactionId))
        );
    }

    @Test
    void shouldConflictOnConcurrentModification() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId, new AccountOpenedEvent(ownerId));

        var eventStream1 = new TransactionalEventStream<>(eventStore, (aggregate, version) -> null);
        var account1 = new Account(eventStream1, accountId);
        eventStream1.replay(account1);
        account1.deposit(42, UUID.randomUUID());

        var eventStream2 = new TransactionalEventStream<>(eventStore, (aggregate, version) -> null);
        var account2 = new Account(eventStream2, accountId);
        eventStream2.replay(account2);
        account2.deposit(42, UUID.randomUUID());
        eventStream2.commit();

        assertThatThrownBy(eventStream1::commit).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    void multipleDepositsShouldAccumulateBalance() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId, new AccountOpenedEvent(ownerId));

        var tx1 = UUID.randomUUID();
        accountRepository.transact(accountId, Operation.deposit(1, tx1));
        var tx2 = UUID.randomUUID();
        accountRepository.transact(accountId, Operation.deposit(1, tx2));

        var account = accountRepository.query(accountId);
        assertThat(account.balance()).isEqualTo(2);
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new SequencedEvent<>(accountId, 1, new AccountOpenedEvent(ownerId)),
                new SequencedEvent<>(accountId, 2, new MoneyDepositedEvent(1, 1, tx1)),
                new SequencedEvent<>(accountId, 3, new MoneyDepositedEvent(1, 2, tx2))
        );
    }

    @Test
    void shouldNotDepositZeroToAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId, new AccountOpenedEvent(ownerId));

        var account = accountRepository.query(accountId);
        account.deposit(0, UUID.randomUUID());

        assertThat(account.balance()).isZero();
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new SequencedEvent<>(accountId, 1, new AccountOpenedEvent(ownerId))
        );
    }

    @Test
    void shouldThrowWhenDepositingNegativeAmount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId, new AccountOpenedEvent(ownerId));

        var account = accountRepository.query(accountId);
        assertThatThrownBy(() -> account.deposit(-42, UUID.randomUUID())).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Can not deposit negative amount");
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new SequencedEvent<>(accountId, 1, new AccountOpenedEvent(ownerId))
        );
    }

    @Test
    void shouldWithdrawMoney() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        var depositTxId = UUID.randomUUID();

        givenEvents(accountId,
                new AccountOpenedEvent(ownerId),
                new MoneyDepositedEvent(10, 10, depositTxId)
        );

        var withdrawalTxId = UUID.randomUUID();
        accountRepository.transact(accountId, Operation.withdraw(5, withdrawalTxId));

        var account = accountRepository.query(accountId);
        assertThat(account.balance()).isEqualTo(5);
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new SequencedEvent<>(accountId, 1, new AccountOpenedEvent(ownerId)),
                new SequencedEvent<>(accountId, 2, new MoneyDepositedEvent(10, 10, depositTxId)),
                new SequencedEvent<>(accountId, 3, new MoneyWithdrawnEvent(5, 5, withdrawalTxId))
        );
    }

    @Test
    void shouldNotWithdrawZero() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId, new AccountOpenedEvent(ownerId));

        var account = accountRepository.query(accountId);
        account.withdraw(0, UUID.randomUUID());

        assertThat(account.balance()).isEqualTo(0);
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new SequencedEvent<>(accountId, 1, new AccountOpenedEvent(ownerId))
        );
    }

    @Test
    void shouldNotWithdrawMoneyWhenBalanceInsufficient() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        givenEvents(accountId,
                new AccountOpenedEvent(ownerId),
                new MoneyDepositedEvent(10, 10, UUID.randomUUID())
        );

        var account = accountRepository.query(accountId);
        assertThatThrownBy(() -> account.withdraw(11, UUID.randomUUID())).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    void shouldCreateSnapshotAfterFiveEvents() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        givenEvents(accountId,
                new AccountOpenedEvent(ownerId),
                new MoneyDepositedEvent(10, 10, UUID.randomUUID())
        );
        snapshottingAccountRepository.transact(accountId, Operation.deposit(5, UUID.randomUUID()));
        snapshottingAccountRepository.transact(accountId, Operation.deposit(5, UUID.randomUUID()));
        snapshottingAccountRepository.transact(accountId, Operation.deposit(5, UUID.randomUUID()));

        var snapshot = eventStore.loadSnapshot(accountId);
        assertThat(snapshot)
                .isEqualTo(new SequencedEvent<>(accountId, 5, new AccountSnapshot(accountId, ownerId, 25, true)));

        snapshottingAccountRepository.transact(accountId, Operation.deposit(5, UUID.randomUUID()));
        snapshottingAccountRepository.transact(accountId, Operation.deposit(5, UUID.randomUUID()));
        snapshottingAccountRepository.transact(accountId, Operation.deposit(5, UUID.randomUUID()));
        snapshottingAccountRepository.transact(accountId, Operation.deposit(5, UUID.randomUUID()));
        snapshottingAccountRepository.transact(accountId, Operation.deposit(5, UUID.randomUUID()));

        snapshot = eventStore.loadSnapshot(accountId);
        assertThat(snapshot)
                .isEqualTo(new SequencedEvent<>(accountId, 10, new AccountSnapshot(accountId, ownerId, 50, true)));
    }

    @Test
    void shouldInstantiateAccountFromSnapshot() {
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
    void shouldNotReplayEventsPriorToSnapshot() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        eventStore.append(
                List.of(new SequencedEvent<>(accountId, 10, new MoneyDepositedEvent(10, 11, UUID.randomUUID()))),
                new SequencedEvent<>(accountId, 10, new AccountSnapshot(accountId, ownerId, 42, true)));

        var account = snapshottingAccountRepository.query(accountId);
        assertThat(account.balance()).isEqualTo(42);
        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
    }

    @Test
    void shouldReplayEventsAfterSnapshot() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        eventStore.append(
                List.of(new SequencedEvent<>(accountId, 10, new MoneyDepositedEvent(10, 11, UUID.randomUUID())),
                        new SequencedEvent<>(accountId, 11, new MoneyDepositedEvent(1, 43, UUID.randomUUID()))),
                new SequencedEvent<>(accountId, 10, new AccountSnapshot(accountId, ownerId, 42, true)));

        var account = snapshottingAccountRepository.query(accountId);
        assertThat(account.balance()).isEqualTo(43);
        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
    }

}
