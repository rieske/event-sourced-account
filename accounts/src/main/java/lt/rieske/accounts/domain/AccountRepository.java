package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventsourcing.EventStream;

import java.util.UUID;

public class AccountRepository {

    private final EventStore<Account> eventStore;

    public AccountRepository(EventStore<Account> eventStore) {
        this.eventStore = eventStore;
    }

    public Account loadAccount(UUID accountId) {
        var eventStream = new EventStream<>(eventStore, accountId);
        var account = new Account(eventStream);
        eventStream.replay(account);
        return account;
    }

    public Account newAccount(UUID accountId) {
        var accountEventStream = new EventStream<>(eventStore, accountId);
        return new Account(accountEventStream);
    }
}
