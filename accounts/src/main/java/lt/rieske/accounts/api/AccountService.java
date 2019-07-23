package lt.rieske.accounts.api;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.domain.AccountSnapshot;
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

    AccountSnapshot queryAccount(UUID accountId) {
        return accountRepository.query(accountId).snapshot();
    }
}
