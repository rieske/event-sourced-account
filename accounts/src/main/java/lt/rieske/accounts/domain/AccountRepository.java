package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.AggregateRepository;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventsourcing.EventStream;

public class AccountRepository extends AggregateRepository<Account> {

    public AccountRepository(EventStore<Account> eventStore) {
        super(eventStore);
    }

    @Override
    protected Account makeAggregate(EventStream<Account> eventStream) {
        return new Account(eventStream);
    }
}
