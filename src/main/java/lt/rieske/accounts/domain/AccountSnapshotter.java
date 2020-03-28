package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.Snapshotter;

public class AccountSnapshotter implements Snapshotter<Account, AccountEventsVisitor> {

    private final int snapshottingFrequencyEvents;

    public AccountSnapshotter(int snapshottingFrequencyEvents) {
        this.snapshottingFrequencyEvents = snapshottingFrequencyEvents;
    }

    @Override
    public AccountSnapshot takeSnapshot(Account account, long version) {
        if (version % snapshottingFrequencyEvents == 0) {
            return account.snapshot();
        }
        return null;
    }
}
