package lt.rieske.accounts.eventsourcing;

import com.mysql.cj.jdbc.MysqlDataSource;
import lt.rieske.accounts.eventstore.BlobEventStore;
import lt.rieske.accounts.eventstore.Configuration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.MySQLContainer;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import static lt.rieske.accounts.eventstore.MySqlEventStore.uuidToBytes;

@Tag("integration")
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

    protected BlobEventStore blobEventStore(DataSource dataSource) {
        return Configuration.mysqlEventStore(dataSource);
    }

    @Override
    protected void setUUID(PreparedStatement statement, int column, UUID uuid) throws SQLException {
        statement.setBytes(column, uuidToBytes(uuid));
    }

    static class MySql {

        private static final String DATABASE = "event_store";

        private final MySQLContainer mysql = new MySQLContainer()
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
