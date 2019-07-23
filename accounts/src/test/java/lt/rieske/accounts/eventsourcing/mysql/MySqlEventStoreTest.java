package lt.rieske.accounts.eventsourcing.mysql;

import lt.rieske.accounts.infrastructure.SqlEventStoreTest;
import org.junit.jupiter.api.AfterAll;

import javax.sql.DataSource;

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
