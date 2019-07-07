package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.AggregateRepository;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventsourcing.EventStream;

public class SnapshottingAccountRepository extends AggregateRepository<Account> {

    public SnapshottingAccountRepository(EventStore<Account> eventStore) {
        super(eventStore, new AccountSnapshotter());
    }

    @Override
    protected Account makeAggregate(EventStream<Account> eventStream) {
        return new Account(eventStream);
    }
}
