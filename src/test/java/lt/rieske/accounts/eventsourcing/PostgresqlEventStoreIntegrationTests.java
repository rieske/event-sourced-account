package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.eventstore.BlobEventStore;
import lt.rieske.accounts.eventstore.Configuration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

@Tag("integration")
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
    protected BlobEventStore blobEventStore(DataSource dataSource) {
        return Configuration.postgresEventStore(dataSource);
    }

    @Override
    protected void setUUID(PreparedStatement statement, int column, UUID uuid) throws SQLException {
        statement.setObject(column, uuid);
    }

    static class Postgresql {

        private static final String DATABASE = "event_store";

        private final PostgreSQLContainer postgresql = new PostgreSQLContainer()
                .withDatabaseName(DATABASE);

        private final DataSource dataSource;

        Postgresql() {
            postgresql.withTmpFs(Map.of("/var/lib/postgresql/data", "rw"));

            postgresql.start();

            var dataSource = new PGSimpleDataSource();

            dataSource.setUrl(postgresql.getJdbcUrl());
            dataSource.setUser(postgresql.getUsername());
            dataSource.setPassword(postgresql.getPassword());
            dataSource.setDatabaseName(DATABASE);
            dataSource.setCurrentSchema(DATABASE);

            this.dataSource = dataSource;

            var flyway = Flyway.configure().dataSource(dataSource).locations("db/postgresql")
                    .schemas(DATABASE)
                    .defaultSchema(DATABASE)
                    .load();
            flyway.migrate();
        }

        void stop() {
            postgresql.stop();
        }

        DataSource dataSource() {
            return dataSource;
        }
    }
}
