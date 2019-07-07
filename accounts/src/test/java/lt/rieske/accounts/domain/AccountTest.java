package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.Event;
import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AccountTest {

    @Test
    public void shouldOpenAnAccount() {
        var accountId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();

        var account = new Account(Event::apply);
        account.open(accountId, ownerId);

        assertThat(account.id()).isEqualTo(accountId);
        assertThat(account.ownerId()).isEqualTo(ownerId);
        assertThat(account.balance()).isZero();
    }

    @Test
    public void shouldDepositMoneyToAccount() {
        var account = new Account(Event::apply);
        account.open(UUID.randomUUID(), UUID.randomUUID());
        account.deposit(42);

        assertThat(account.balance()).isEqualTo(42);
    }

    @Test
    public void multipleDepositsShouldAccumulateBalance() {
        var account = new Account(Event::apply);
        account.open(UUID.randomUUID(), UUID.randomUUID());
        account.deposit(1);
        account.deposit(1);

        assertThat(account.balance()).isEqualTo(2);
    }

    @Test
    public void shouldThrowWhenDepositingNegativeAmount() {
        var account = new Account(Event::apply);
        account.open(UUID.randomUUID(), UUID.randomUUID());

        assertThatThrownBy(() -> account.deposit(-42)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Can not deposit negative amount");
    }

    @Test
    public void shouldWithdrawMoney() {
        var account = new Account(Event::apply);
        account.open(UUID.randomUUID(), UUID.randomUUID());
        account.deposit(10);

        account.withdraw(5);

        assertThat(account.balance()).isEqualTo(5);
    }

    @Test
    public void shouldNotWithdrawMoneyWhenBalanceInsufficient() {
        var account = new Account(Event::apply);
        account.open(UUID.randomUUID(), UUID.randomUUID());

        assertThatThrownBy(() -> account.withdraw(1)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    public void shouldCloseAccount() {
        var account = new Account(Event::apply);
        account.open(UUID.randomUUID(), UUID.randomUUID());

        account.close();

        assertThat(account.isOpen()).isFalse();
    }

    @Test
    public void shouldNotCloseAccountWithOutstandingBalance() {
        var account = new Account(Event::apply);
        account.open(UUID.randomUUID(), UUID.randomUUID());
        account.deposit(10);

        assertThatThrownBy(account::close).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Balance outstanding");
    }

}
