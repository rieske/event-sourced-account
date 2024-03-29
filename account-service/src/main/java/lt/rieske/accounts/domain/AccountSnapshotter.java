package lt.rieske.accounts.domain;

import lt.rieske.accounts.eventsourcing.Snapshotter;

public class AccountSnapshotter implements Snapshotter<Account, AccountEvent> {

    private final int snapshottingFrequencyEvents;

    public AccountSnapshotter(int snapshottingFrequencyEvents) {
        this.snapshottingFrequencyEvents = snapshottingFrequencyEvents;
    }

    @Override
    public AccountEvent.AccountSnapshot takeSnapshot(Account account, long version) {
        if (version % snapshottingFrequencyEvents == 0) {
            return account.snapshot();
        }
        return null;
    }
}
