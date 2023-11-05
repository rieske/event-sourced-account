package lt.rieske.accounts.api;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.domain.AccountEvent;
import lt.rieske.accounts.domain.AtomicOperation;
import lt.rieske.accounts.eventsourcing.AggregateRepository;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventsourcing.SequencedEvent;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.UUID;

class AccountService {

    private final AggregateRepository<Account, AccountEvent> accountRepository;
    private final EventStore<AccountEvent> eventStore;

    AccountService(AggregateRepository<Account, AccountEvent> accountRepository, EventStore<AccountEvent> eventStore) {
        this.accountRepository = accountRepository;
        this.eventStore = eventStore;
    }

    void openAccount(UUID accountId, UUID ownerId) {
        accountRepository.create(accountId, UUID.randomUUID(), AtomicOperation.open(ownerId));
    }

    void deposit(UUID accountId, long amount, UUID transactionId) {
        withRetryOnConcurrentModification(() ->
                accountRepository.transact(accountId, transactionId, AtomicOperation.deposit(amount)));
    }

    void withdraw(UUID accountId, long amount, UUID transactionId) {
        withRetryOnConcurrentModification(() ->
                accountRepository.transact(accountId, transactionId, AtomicOperation.withdraw(amount)));
    }

    void transfer(UUID sourceAccountId, UUID targetAccountId, long amount, UUID transactionId) {
        withRetryOnConcurrentModification(() ->
                accountRepository.transact(sourceAccountId, targetAccountId, transactionId, AtomicOperation.transfer(amount)));
    }

    void close(UUID accountId) {
        accountRepository.transact(accountId, UUID.randomUUID(), AtomicOperation.close());
    }

    AccountEvent.AccountSnapshot queryAccount(UUID accountId) {
        return accountRepository.query(accountId).snapshot();
    }

    List<SequencedEvent<AccountEvent>> getEvents(UUID accountId) {
        return eventStore.getEvents(accountId, 0).toList();
    }

    private static void withRetryOnConcurrentModification(Runnable r) {
        ConcurrentModificationException concurrentModification = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                r.run();
                return;
            } catch (ConcurrentModificationException e) {
                concurrentModification = e;
            }
        }
        throw concurrentModification;
    }
}
