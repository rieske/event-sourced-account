package lt.rieske.accounts.api;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.domain.AccountSnapshot;
import lt.rieske.accounts.domain.Operation;
import lt.rieske.accounts.eventsourcing.AggregateRepository;

import java.util.UUID;

class AccountService {

    private final AggregateRepository<Account> accountRepository;

    AccountService(AggregateRepository<Account> accountRepository) {
        this.accountRepository = accountRepository;
    }

    void openAccount(UUID accountId, UUID ownerId) {
        accountRepository.create(accountId, UUID.randomUUID(), Operation.open(ownerId));
    }

    void deposit(UUID accountId, long amount, UUID transactionId) {
        accountRepository.transact(accountId, transactionId, Operation.deposit(amount));
    }

    void withdraw(UUID accountId, long amount, UUID transactionId) {
        accountRepository.transact(accountId, transactionId, Operation.withdraw(amount));
    }

    void transfer(UUID sourceAccountId, UUID targetAccountId, long amount, UUID transactionId) {
        accountRepository.transact(sourceAccountId, targetAccountId, transactionId, Operation.transfer(amount));
    }

    void close(UUID accountId) {
        accountRepository.transact(accountId, UUID.randomUUID(), Operation.close());
    }

    AccountSnapshot queryAccount(UUID accountId) {
        return accountRepository.query(accountId).snapshot();
    }
}
