package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.api.ApiConfiguration;
import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.domain.AccountEvent;
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

    private EventStore<AccountEvent> eventStore;
    private AggregateRepository<Account, AccountEvent> accountRepository;
    private AggregateRepository<Account, AccountEvent> snapshottingAccountRepository;

    protected abstract EventStore<AccountEvent> getEventStore();

    @BeforeEach
    void init() {
        eventStore = getEventStore();
        accountRepository = ApiConfiguration.accountRepository(eventStore);
        snapshottingAccountRepository = ApiConfiguration.snapshottingAccountRepository(eventStore, 5);
    }

    private void givenEvents(UUID accountId, UUID transactionId, AccountEvent... events) {
        List<SequencedEvent<AccountEvent>> sequencedEvents = new ArrayList<>();
        for (int i = 0; i < events.length; i++) {
            sequencedEvents.add(new SequencedEvent<>(accountId, i + 1, null, events[i]));
        }
        eventStore.append(sequencedEvents, List.of(), transactionId);
    }

    @Test
    void shouldOpenAnAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        var transactionId = UUID.randomUUID();
        accountRepository.create(accountId, transactionId, Operation.open(ownerId));
        var account = accountRepository.query(accountId);

        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
        assertThat(account.balance()).isZero();
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new SequencedEvent<>(accountId, 1, transactionId, new AccountOpenedEvent(ownerId)));
    }

    @Test
    void shouldConflictIfAccountWithIdExists() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        var transactionId = UUID.randomUUID();
        accountRepository.create(accountId, transactionId, Operation.open(ownerId));

        assertThatThrownBy(() -> accountRepository.create(accountId, transactionId, Operation.open(UUID.randomUUID())))
                .isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    void shouldLoadAnAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId, UUID.randomUUID(), new AccountOpenedEvent(ownerId));

        var account = accountRepository.query(accountId);

        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
    }

    @Test
    void shouldLoadTwoDistinctAccounts() {
        var account1Id = UUID.randomUUID();
        var account2Id = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(account1Id, UUID.randomUUID(), new AccountOpenedEvent(ownerId));
        givenEvents(account2Id, UUID.randomUUID(), new AccountOpenedEvent(ownerId));

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
        var openTxId = UUID.randomUUID();
        givenEvents(accountId, openTxId, new AccountOpenedEvent(ownerId));

        var transactionId = UUID.randomUUID();
        accountRepository.transact(accountId, transactionId, Operation.deposit(42));

        var account = accountRepository.query(accountId);
        assertThat(account.balance()).isEqualTo(42);
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new SequencedEvent<>(accountId, 1, openTxId, new AccountOpenedEvent(ownerId)),
                new SequencedEvent<>(accountId, 2, transactionId, new MoneyDepositedEvent(42, 42))
        );
    }

    @Test
    void shouldConflictOnConcurrentModification() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        givenEvents(accountId, UUID.randomUUID(), new AccountOpenedEvent(ownerId));

        var eventStream1 = new TransactionalEventStream<Account, AccountEvent>(eventStore, (aggregate, version) -> null);
        var account1 = new Account(eventStream1, accountId);
        eventStream1.replay(account1, accountId);
        account1.deposit(42);

        var eventStream2 = new TransactionalEventStream<Account, AccountEvent>(eventStore, (aggregate, version) -> null);
        var account2 = new Account(eventStream2, accountId);
        eventStream2.replay(account2, accountId);
        account2.deposit(42);
        eventStream2.commit(UUID.randomUUID());

        assertThatThrownBy(() -> eventStream1.commit(UUID.randomUUID())).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    void multipleDepositsShouldAccumulateBalance() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        var openTxId = UUID.randomUUID();
        givenEvents(accountId, openTxId, new AccountOpenedEvent(ownerId));

        var tx1 = UUID.randomUUID();
        accountRepository.transact(accountId, tx1, Operation.deposit(1));
        var tx2 = UUID.randomUUID();
        accountRepository.transact(accountId, tx2, Operation.deposit(1));

        var account = accountRepository.query(accountId);
        assertThat(account.balance()).isEqualTo(2);
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new SequencedEvent<>(accountId, 1, openTxId, new AccountOpenedEvent(ownerId)),
                new SequencedEvent<>(accountId, 2, tx1, new MoneyDepositedEvent(1, 1)),
                new SequencedEvent<>(accountId, 3, tx2, new MoneyDepositedEvent(1, 2))
        );
    }

    @Test
    void shouldNotDepositZeroToAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        var openTxId = UUID.randomUUID();
        givenEvents(accountId, openTxId, new AccountOpenedEvent(ownerId));

        var account = accountRepository.query(accountId);
        account.deposit(0);

        assertThat(account.balance()).isZero();
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new SequencedEvent<>(accountId, 1, openTxId, new AccountOpenedEvent(ownerId))
        );
    }

    @Test
    void shouldThrowWhenDepositingNegativeAmount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        var openTxId = UUID.randomUUID();
        givenEvents(accountId, openTxId, new AccountOpenedEvent(ownerId));

        var account = accountRepository.query(accountId);
        assertThatThrownBy(() -> account.deposit(-42)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Can not deposit negative amount");
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new SequencedEvent<>(accountId, 1, openTxId, new AccountOpenedEvent(ownerId))
        );
    }

    @Test
    void shouldWithdrawMoney() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        var openTxId = UUID.randomUUID();

        givenEvents(accountId, openTxId,
                new AccountOpenedEvent(ownerId),
                new MoneyDepositedEvent(10, 10)
        );

        var withdrawalTxId = UUID.randomUUID();
        accountRepository.transact(accountId, withdrawalTxId, Operation.withdraw(5));

        var account = accountRepository.query(accountId);
        assertThat(account.balance()).isEqualTo(5);
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new SequencedEvent<>(accountId, 1, openTxId, new AccountOpenedEvent(ownerId)),
                new SequencedEvent<>(accountId, 2, openTxId, new MoneyDepositedEvent(10, 10)),
                new SequencedEvent<>(accountId, 3, withdrawalTxId, new MoneyWithdrawnEvent(5, 5))
        );
    }

    @Test
    void shouldNotWithdrawZero() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        var openTxId = UUID.randomUUID();
        givenEvents(accountId, openTxId, new AccountOpenedEvent(ownerId));

        var account = accountRepository.query(accountId);
        account.withdraw(0);

        assertThat(account.balance()).isEqualTo(0);
        assertThat(eventStore.getEvents(accountId, 0)).containsExactly(
                new SequencedEvent<>(accountId, 1, openTxId, new AccountOpenedEvent(ownerId))
        );
    }

    @Test
    void shouldNotWithdrawMoneyWhenBalanceInsufficient() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        givenEvents(accountId, UUID.randomUUID(),
                new AccountOpenedEvent(ownerId),
                new MoneyDepositedEvent(10, 10)
        );

        var account = accountRepository.query(accountId);
        assertThatThrownBy(() -> account.withdraw(11)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    void shouldCreateSnapshotAfterFiveEvents() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        givenEvents(accountId, UUID.randomUUID(),
                new AccountOpenedEvent(ownerId),
                new MoneyDepositedEvent(10, 10)
        );
        snapshottingAccountRepository.transact(accountId, UUID.randomUUID(), Operation.deposit(5));
        snapshottingAccountRepository.transact(accountId, UUID.randomUUID(), Operation.deposit(5));
        snapshottingAccountRepository.transact(accountId, UUID.randomUUID(), Operation.deposit(5));

        var snapshot = eventStore.loadSnapshot(accountId);
        assertThat(snapshot.aggregateId()).isEqualTo(accountId);
        assertThat(snapshot.sequenceNumber()).isEqualTo(5);
        assertThat(snapshot.event()).isInstanceOf(AccountSnapshot.class);
        AccountSnapshot snapshotEvent = (AccountSnapshot) snapshot.event();
        assertThat(snapshotEvent.accountId()).isEqualTo(accountId);
        assertThat(snapshotEvent.ownerId()).isEqualTo(ownerId);
        assertThat(snapshotEvent.balance()).isEqualTo(25);
        assertThat(snapshotEvent.open()).isTrue();

        snapshottingAccountRepository.transact(accountId, UUID.randomUUID(), Operation.deposit(5));
        snapshottingAccountRepository.transact(accountId, UUID.randomUUID(), Operation.deposit(5));
        snapshottingAccountRepository.transact(accountId, UUID.randomUUID(), Operation.deposit(5));
        snapshottingAccountRepository.transact(accountId, UUID.randomUUID(), Operation.deposit(5));
        snapshottingAccountRepository.transact(accountId, UUID.randomUUID(), Operation.deposit(5));

        snapshot = eventStore.loadSnapshot(accountId);
        assertThat(snapshot.aggregateId()).isEqualTo(accountId);
        assertThat(snapshot.sequenceNumber()).isEqualTo(10);
        assertThat(snapshot.event()).isInstanceOf(AccountSnapshot.class);
        snapshotEvent = (AccountSnapshot) snapshot.event();
        assertThat(snapshotEvent.accountId()).isEqualTo(accountId);
        assertThat(snapshotEvent.ownerId()).isEqualTo(ownerId);
        assertThat(snapshotEvent.balance()).isEqualTo(50);
        assertThat(snapshotEvent.open()).isTrue();
    }

    @Test
    void shouldInstantiateAccountFromSnapshot() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        eventStore.append(List.of(),
                List.of(new SequencedEvent<>(accountId, 10, null, new AccountSnapshot(accountId, ownerId, 42, true))),
                null);

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
                List.of(new SequencedEvent<>(accountId, 10, null, new MoneyDepositedEvent(10, 11))),
                List.of(new SequencedEvent<>(accountId, 10, null, new AccountSnapshot(accountId, ownerId, 42, true))),
                UUID.randomUUID());

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
                List.of(new SequencedEvent<>(accountId, 10, null, new MoneyDepositedEvent(10, 11)),
                        new SequencedEvent<>(accountId, 11, null, new MoneyDepositedEvent(1, 43))),
                List.of(new SequencedEvent<>(accountId, 10, null, new AccountSnapshot(accountId, ownerId, 42, true))),
                UUID.randomUUID());

        var account = snapshottingAccountRepository.query(accountId);
        assertThat(account.balance()).isEqualTo(43);
        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
    }

}
