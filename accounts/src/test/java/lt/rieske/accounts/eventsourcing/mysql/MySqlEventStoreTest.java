package lt.rieske.accounts.eventsourcing.mysql;

import lt.rieske.accounts.eventstore.SqlEventStoreTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;

import javax.sql.DataSource;

@Tag("integration")
class MySqlEventStoreTest extends SqlEventStoreTest {

    private static final MySql MYSQL = new MySql();

    protected DataSource dataSource() {
        return MYSQL.dataSource();
    }

    @AfterAll
    static void stopDatabase() {
        MYSQL.stop();
    }
}
