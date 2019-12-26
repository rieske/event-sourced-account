package lt.rieske.accounts.eventsourcing;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.util.Map;

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

            this.dataSource = dataSource;

            var flyway = Flyway.configure().dataSource(dataSource).locations("db/postgresql").load();
            System.out.println(flyway.getConfiguration().getLocations()[0]);
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
