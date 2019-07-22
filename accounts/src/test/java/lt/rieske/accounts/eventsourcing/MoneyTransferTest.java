package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.domain.AccountSnapshotter;
import lt.rieske.accounts.domain.Operation;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class MoneyTransferTest {

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

    @Test
    public void shouldTransferMoneyBetweenAccounts() {
        var ownerId = UUID.randomUUID();

        var sourceAccountId = UUID.randomUUID();
        accountRepository.create(sourceAccountId, account -> account.open(ownerId));

        var targetAccountId = UUID.randomUUID();
        accountRepository.create(targetAccountId, account -> account.open(ownerId));

        int transferAmount = 42;
        accountRepository.transact(sourceAccountId, Operation.deposit(transferAmount));
        accountRepository.transact(sourceAccountId, targetAccountId, Operation.transfer(transferAmount));

        var sourceAccount = accountRepository.query(sourceAccountId);
        assertThat(sourceAccount.balance()).isZero();

        var targetAccount = accountRepository.query(targetAccountId);
        assertThat(targetAccount.balance()).isEqualTo(transferAmount);
    }
}
