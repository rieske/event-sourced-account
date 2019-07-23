package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.infrastructure.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class IdempotencyTest {

    private AggregateRepository<Account> accountRepository;

    protected abstract EventStore<Account> getEventStore();

    private final UUID ownerId = UUID.randomUUID();
    private final UUID sourceAccountId = UUID.randomUUID();
    private final UUID targetAccountId = UUID.randomUUID();

    @BeforeEach
    void init() {
        accountRepository = Configuration.accountRepository(getEventStore());

        accountRepository.create(sourceAccountId, account -> account.open(ownerId));
        accountRepository.create(targetAccountId, account -> account.open(ownerId));
    }

    @Test
    void depositTransactionShouldBeIdempotent() {
        var accountId = UUID.randomUUID();
        accountRepository.create(accountId, account -> account.open(ownerId));

        var transactionId = UUID.randomUUID();
        accountRepository.transact(accountId, account -> account.deposit(10, transactionId));
        accountRepository.transact(accountId, account -> account.deposit(10, transactionId));

        var account = accountRepository.query(accountId);

        assertThat(account.balance()).isEqualTo(10);
    }
}
