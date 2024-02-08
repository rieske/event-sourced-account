package lt.rieske.accounts.api;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.domain.AccountEvent;
import lt.rieske.accounts.domain.AccountSnapshotter;
import lt.rieske.accounts.eventsourcing.AggregateRepository;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventstore.BlobEventStore;
import lt.rieske.accounts.eventstore.Configuration;

import java.util.function.Supplier;

public class ApiConfiguration {

    public static Server server(Supplier<BlobEventStore> eventStoreSupplier) {
        var eventStore = Configuration.accountEventStore(eventStoreSupplier.get());
        var accountRepository = snapshottingAccountRepository(eventStore, 50);
        var accountService = new AccountService(accountRepository, eventStore);
        var accountResource = new AccountResource(accountService);

        return new Server(accountResource);
    }

    public static AggregateRepository<Account, AccountEvent> accountRepository(EventStore<AccountEvent> eventStore) {
        return new AggregateRepository<>(eventStore, Account::new);
    }

    public static AggregateRepository<Account, AccountEvent> snapshottingAccountRepository(
            EventStore<AccountEvent> eventStore, int snapshottingFrequency) {
        return new AggregateRepository<>(eventStore, Account::new, new AccountSnapshotter(snapshottingFrequency));
    }

    private ApiConfiguration() {
    }

}
