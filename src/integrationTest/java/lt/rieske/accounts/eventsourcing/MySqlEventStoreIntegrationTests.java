package lt.rieske.accounts.eventsourcing;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.testcontainers.containers.MySQLContainer;

import javax.sql.DataSource;
import java.util.Map;

class MySqlEventStoreIntegrationTests extends SqlEventStoreIntegrationTests {

    private static final MySql MYSQL = new MySql();

    @AfterAll
    static void stopDatabase() {
        MYSQL.stop();
    }

    @Override
    protected DataSource dataSource() {
        return MYSQL.dataSource();
    }

    static class MySql {

        private static final String DATABASE = "event_store";

        private final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.19")
                .withDatabaseName(DATABASE);

        private final DataSource dataSource;

        MySql() {
            mysql.withTmpFs(Map.of("/var/lib/mysql", "rw"));

            mysql.start();

            var dataSource = new MysqlDataSource();

            dataSource.setUrl(mysql.getJdbcUrl());
            dataSource.setUser(mysql.getUsername());
            dataSource.setPassword(mysql.getPassword());
            dataSource.setDatabaseName(DATABASE);

            this.dataSource = dataSource;

            var flyway = Flyway.configure().dataSource(dataSource).load();
            flyway.migrate();
        }

        void stop() {
            mysql.stop();
        }

        DataSource dataSource() {
            return dataSource;
        }
    }
}