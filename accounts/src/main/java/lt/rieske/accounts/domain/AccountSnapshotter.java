package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.Event;
import lt.rieske.accounts.eventsourcing.Snapshotter;

public class AccountSnapshotter implements Snapshotter<Account> {

    private static final int SNAPSHOT_FREQUENCY_EVENTS = 50;

    @Override
    public Event<Account> takeSnapshot(Account account, long version) {
        if (version % SNAPSHOT_FREQUENCY_EVENTS == 0) {
            return new AccountSnapshot(account.id(), account.ownerId(), account.balance(), account.isOpen());
        }
        return null;
    }
}
