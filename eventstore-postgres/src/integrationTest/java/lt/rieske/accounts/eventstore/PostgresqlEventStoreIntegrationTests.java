package lt.rieske.accounts.eventstore;

import org.junit.jupiter.api.AfterAll;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

class PostgresqlEventStoreIntegrationTests extends SqlEventStoreIntegrationTests {

    private static final Postgresql POSTGRESQL = new Postgresql();

    @AfterAll
    static void stopDatabase() {
        POSTGRESQL.stop();
    }

    @Override
    protected DataSource dataSource() {
        return POSTGRESQL.dataSource();
    }

    @Override
    protected BlobEventStore blobEventStore() {
        return EventStoreFactory.makeEventStore(POSTGRESQL.jdbcUrl(), POSTGRESQL.username(), POSTGRESQL.password(), Function.identity());
    }

    @Override
    protected void setUUID(PreparedStatement statement, int column, UUID uuid) throws SQLException {
        statement.setObject(column, uuid);
    }

    static class Postgresql {

        private final PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:12.4")
                .withDatabaseName("event_store");

        Postgresql() {
            postgresql.withTmpFs(Map.of("/var/lib/postgresql/data", "rw"));

            postgresql.start();
        }

        void stop() {
            postgresql.stop();
        }

        DataSource dataSource() {
            var dataSource = new PGSimpleDataSource();
            dataSource.setUrl(jdbcUrl());
            dataSource.setUser(username());
            dataSource.setPassword(password());
            dataSource.setDatabaseName(postgresql.getDatabaseName());
            dataSource.setCurrentSchema(postgresql.getDatabaseName());
            return dataSource;
        }

        String jdbcUrl() {
            return postgresql.getJdbcUrl();
        }

        String username() {
            return postgresql.getUsername();
        }

        String password() {
            return postgresql.getPassword();
        }
    }
}
