package lt.rieske.accounts.eventstore.mysql;

import com.mysql.cj.jdbc.MysqlDataSource;
import lt.rieske.accounts.eventstore.BlobEventStore;
import lt.rieske.accounts.eventstore.SqlEventStoreIntegrationTests;
import org.junit.jupiter.api.AfterAll;
import org.testcontainers.containers.MySQLContainer;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

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

    @Override
    protected BlobEventStore blobEventStore() {
        return new MySqlEventStoreFactory().makeEventStore(MYSQL.jdbcUrl(), MYSQL.username(), MYSQL.password(), Function.identity());
    }

    @Override
    protected void setUUID(PreparedStatement statement, int column, UUID uuid) throws SQLException {
        statement.setBytes(column, MySqlEventStore.uuidToBytes(uuid));
    }

    static class MySql {

        private final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.21")
                .withDatabaseName("event_store");

        MySql() {
            mysql.withTmpFs(Map.of("/var/lib/mysql", "rw"));

            mysql.start();
        }

        void stop() {
            mysql.stop();
        }

        DataSource dataSource() {
            var dataSource = new MysqlDataSource();

            dataSource.setUrl(jdbcUrl());
            dataSource.setUser(username());
            dataSource.setPassword(password());
            dataSource.setDatabaseName(mysql.getDatabaseName());
            return dataSource;
        }

        String jdbcUrl() {
            return mysql.getJdbcUrl();
        }

        String username() {
            return mysql.getUsername();
        }

        String password() {
            return mysql.getPassword();
        }
    }
}
