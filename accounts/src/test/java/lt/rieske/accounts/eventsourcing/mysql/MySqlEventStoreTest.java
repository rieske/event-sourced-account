package lt.rieske.accounts.eventsourcing.mysql;

import lt.rieske.accounts.infrastructure.SqlEventStoreTest;
import org.junit.AfterClass;

import javax.sql.DataSource;

public class MySqlEventStoreTest extends SqlEventStoreTest {

    private static final MySql MYSQL = new MySql();

    protected DataSource dataSource() {
        return MYSQL.dataSource();
    }

    @AfterClass
    public static void stopDatabase() {
        MYSQL.stop();
    }
}
