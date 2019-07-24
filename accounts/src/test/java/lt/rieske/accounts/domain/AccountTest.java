package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.Event;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    @Test
    void shouldOpenAnAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        var account = new Account(Event::apply, accountId);
        account.open(ownerId, UUID.randomUUID());

        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
        assertThat(account.balance()).isZero();
    }

    @Test
    void shouldThrowWhenOpeningAlreadyOpenAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        var account = new Account(Event::apply, accountId);
        account.open(ownerId, UUID.randomUUID());

        assertThatThrownBy(() -> account.open(UUID.randomUUID(), UUID.randomUUID())).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Account already has an owner");
    }

    @Test
    void shouldDepositMoneyToAccount() {
        var accountId = UUID.randomUUID();
        var account = new Account(Event::apply, accountId);
        account.open(UUID.randomUUID(), UUID.randomUUID());
        account.deposit(42, UUID.randomUUID());

        assertThat(account.balance()).isEqualTo(42);
    }

    @Test
    void multipleDepositsShouldAccumulateBalance() {
        var accountId = UUID.randomUUID();
        var account = new Account(Event::apply, accountId);
        account.open(UUID.randomUUID(), UUID.randomUUID());
        account.deposit(1, UUID.randomUUID());
        account.deposit(1, UUID.randomUUID());

        assertThat(account.balance()).isEqualTo(2);
    }

    @Test
    void shouldThrowWhenDepositingNegativeAmount() {
        var accountId = UUID.randomUUID();
        var account = new Account(Event::apply, accountId);
        account.open(UUID.randomUUID(), UUID.randomUUID());

        assertThatThrownBy(() -> account.deposit(-42, UUID.randomUUID())).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Can not deposit negative amount");
    }

    @Test
    void shouldWithdrawMoney() {
        var accountId = UUID.randomUUID();
        var account = new Account(Event::apply, accountId);
        account.open(UUID.randomUUID(), UUID.randomUUID());
        account.deposit(10, UUID.randomUUID());

        account.withdraw(5, UUID.randomUUID());

        assertThat(account.balance()).isEqualTo(5);
    }

    @Test
    void shouldNotWithdrawMoneyWhenBalanceInsufficient() {
        var accountId = UUID.randomUUID();
        var account = new Account(Event::apply, accountId);
        account.open(UUID.randomUUID(), UUID.randomUUID());

        assertThatThrownBy(() -> account.withdraw(1, UUID.randomUUID())).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    void shouldThrowWhenWithdrawingNegativeAmount() {
        var accountId = UUID.randomUUID();
        var account = new Account(Event::apply, accountId);
        account.open(UUID.randomUUID(), UUID.randomUUID());
        account.deposit(100, UUID.randomUUID());

        assertThatThrownBy(() -> account.withdraw(-42, UUID.randomUUID())).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Can not withdraw negative amount");
    }

    @Test
    void shouldCloseAccount() {
        var accountId = UUID.randomUUID();
        var account = new Account(Event::apply, accountId);
        account.open(UUID.randomUUID(), UUID.randomUUID());

        account.close(UUID.randomUUID());

        assertThat(account.isOpen()).isFalse();
    }

    @Test
    void shouldNotCloseAccountWithOutstandingBalance() {
        var accountId = UUID.randomUUID();
        var account = new Account(Event::apply, accountId);
        account.open(UUID.randomUUID(), UUID.randomUUID());
        account.deposit(10, UUID.randomUUID());

        assertThatThrownBy(() -> account.close(UUID.randomUUID())).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Balance outstanding");
    }

}
