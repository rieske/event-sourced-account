package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.AggregateNotFoundException;
import lt.rieske.accounts.eventsourcing.EventStream;
import lt.rieske.accounts.eventsourcing.InMemoryEventStore;
import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AccountTest {

    private final InMemoryEventStore<Account> eventStore = new InMemoryEventStore<>();
    private final EventStream<Account> accountEventStream = new EventStream<>(eventStore);

    @Test
    public void shouldOpenAnAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        var account = new Account(accountEventStream);
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
        eventStore.append(new AccountOpenedEvent(accountId, ownerId));

        var account = new Account(accountEventStream, accountId);

        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
    }

    @Test
    public void shouldLoadTwoDistinctAccounts() {
        var account1Id = UUID.randomUUID();
        var account2Id = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        eventStore.append(new AccountOpenedEvent(account1Id, ownerId));
        eventStore.append(new AccountOpenedEvent(account2Id, ownerId));

        var account1 = new Account(accountEventStream, account1Id);
        var account2 = new Account(accountEventStream, account2Id);

        assertThat(account1.id()).isEqualTo(account1Id);
        assertThat(account1.ownerId()).isEqualTo(ownerId);

        assertThat(account2.id()).isEqualTo(account2Id);
        assertThat(account2.ownerId()).isEqualTo(ownerId);
    }

    @Test
    public void shouldThrowWhenAccountIsNotFound() {
        assertThatThrownBy(() -> new Account(accountEventStream, UUID.randomUUID()))
                .isInstanceOf(AggregateNotFoundException.class);
    }

    @Test
    public void shouldDepositMoneyToAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        eventStore.append(new AccountOpenedEvent(accountId, ownerId));

        var account = new Account(accountEventStream, accountId);

        account.deposit(42);

        assertThat(account.balance()).isEqualTo(42);
        assertThat(eventStore.getEvents(accountId)).containsExactly(
                new AccountOpenedEvent(accountId, ownerId),
                new MoneyDepositedEvent(accountId, 42)
        );
    }
}
