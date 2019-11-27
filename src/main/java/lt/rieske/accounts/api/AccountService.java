package lt.rieske.accounts.api;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.domain.AccountEventsVisitor;
import lt.rieske.accounts.domain.AccountSnapshot;
import lt.rieske.accounts.domain.Operation;
import lt.rieske.accounts.eventsourcing.AggregateRepository;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventsourcing.SequencedEvent;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

class AccountService {

    private final AggregateRepository<Account, AccountEventsVisitor> accountRepository;
    private final EventStore<AccountEventsVisitor> eventStore;

    AccountService(AggregateRepository<Account, AccountEventsVisitor> accountRepository, EventStore<AccountEventsVisitor> eventStore) {
        this.accountRepository = accountRepository;
        this.eventStore = eventStore;
    }

    void openAccount(UUID accountId, UUID ownerId) {
        accountRepository.create(accountId, UUID.randomUUID(), Operation.open(ownerId));
    }

    void deposit(UUID accountId, long amount, UUID transactionId) {
        withRetryOnConcurrentModification(() ->
                accountRepository.transact(accountId, transactionId, Operation.deposit(amount)));
    }

    void withdraw(UUID accountId, long amount, UUID transactionId) {
        withRetryOnConcurrentModification(() ->
                accountRepository.transact(accountId, transactionId, Operation.withdraw(amount)));
    }

    void transfer(UUID sourceAccountId, UUID targetAccountId, long amount, UUID transactionId) {
        withRetryOnConcurrentModification(() ->
                accountRepository.transact(sourceAccountId, targetAccountId, transactionId, Operation.transfer(amount)));
    }

    void close(UUID accountId) {
        accountRepository.transact(accountId, UUID.randomUUID(), Operation.close());
    }

    AccountSnapshot queryAccount(UUID accountId) {
        return accountRepository.query(accountId).snapshot();
    }

    List<SequencedEvent<AccountEventsVisitor>> getEvents(UUID accountId) {
        return eventStore.getEvents(accountId, 0).collect(Collectors.toUnmodifiableList());
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
