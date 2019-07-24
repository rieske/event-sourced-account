package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.Event;
import lt.rieske.accounts.eventsourcing.Snapshotter;

public class AccountSnapshotter implements Snapshotter<Account> {

    private final int snapshottingFrequencyEvents;

    public AccountSnapshotter(int snapshottingFrequencyEvents) {
        this.snapshottingFrequencyEvents = snapshottingFrequencyEvents;
    }

    @Override
    public Event<Account> takeSnapshot(Account account, long version) {
        if (version % snapshottingFrequencyEvents == 0) {
            return account.snapshot();
        }
        return null;
    }
}
