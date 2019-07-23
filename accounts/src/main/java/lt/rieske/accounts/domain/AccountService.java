package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.AggregateRepository;

import java.util.UUID;

public class AccountService {

    private final AggregateRepository<Account> accountRepository;

    public AccountService(AggregateRepository<Account> accountRepository) {
        this.accountRepository = accountRepository;
    }

    public void openAccount(UUID accountId, UUID ownerId) {
        accountRepository.create(accountId, account -> account.open(ownerId));
    }

    public AccountSnapshot queryAccount(UUID accountId) {
        return accountRepository.query(accountId).snapshot();
    }
}
