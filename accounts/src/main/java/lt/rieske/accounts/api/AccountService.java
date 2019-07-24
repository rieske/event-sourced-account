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
        accountRepository.create(accountId, account -> account.open(ownerId));
    }

    void deposit(UUID accountId, long amount, UUID transactionId) {
        accountRepository.transact(accountId, Operation.deposit(amount, transactionId));
    }

    void withdraw(UUID accountId, long amount, UUID transactionId) {
        accountRepository.transact(accountId, Operation.withdraw(amount, transactionId));
    }

    void transfer(UUID sourceAccountId, UUID targetAccountId, long amount, UUID transactionId) {
        accountRepository.transact(sourceAccountId, targetAccountId, Operation.transfer(amount, transactionId));
    }

    void close(UUID accountId) {
        accountRepository.transact(accountId, Account::close);
    }

    AccountSnapshot queryAccount(UUID accountId) {
        return accountRepository.query(accountId).snapshot();
    }
}
