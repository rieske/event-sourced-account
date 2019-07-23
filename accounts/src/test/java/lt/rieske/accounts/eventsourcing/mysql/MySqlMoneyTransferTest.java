package lt.rieske.accounts.eventsourcing.mysql;

import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventsourcing.MoneyTransferTest;
import lt.rieske.accounts.infrastructure.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;

@Tag("integration")
class MySqlMoneyTransferTest extends MoneyTransferTest {

    private static final MySql MYSQL = new MySql();

    private EventStore<Account> eventStore = Configuration.accountEventStore(MYSQL.dataSource());

    @AfterAll
    static void stopDatabase() {
        MYSQL.stop();
    }

    @Override
    protected EventStore<Account> getEventStore() {
        return eventStore;
    }

}
