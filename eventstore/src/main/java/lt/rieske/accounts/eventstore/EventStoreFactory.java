package lt.rieske.accounts.eventstore;

import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Duration;
import java.util.function.Function;

public final class EventStoreFactory {

    private static final Logger log = LoggerFactory.getLogger(EventStoreFactory.class);

    private EventStoreFactory() {
    }

    public static BlobEventStore makeEventStore(String jdbcUrl, String username, String password, Function<DataSource, DataSource> initializer) {
        return postgresEventStore(jdbcUrl, username, password, initializer);
    }

    static BlobEventStore postgresEventStore(String jdbcUrl, String username, String password, Function<DataSource, DataSource> initializer) {
        return postgresEventStore(postgresDataSource(jdbcUrl, username, password), initializer);
    }

    static BlobEventStore postgresEventStore(DataSource dataSource, Function<DataSource, DataSource> initializer) {
        log.info("Creating PostgreSQL event store");
        migrateDatabase(dataSource);
        return new PostgresEventStore(initializer.apply(dataSource));
    }

    private static DataSource postgresDataSource(String jdbcUrl, String username, String password) {
        var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(jdbcUrl);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    private static void migrateDatabase(DataSource dataSource) {
        waitForDatabaseToBeAvailable(dataSource);
        Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate();
    }

    private static void waitForDatabaseToBeAvailable(DataSource dataSource) {
        var retryPeriod = Duration.ofSeconds(1);
        for (int i = 0; i < 20; i++) {
            try (var conn = dataSource.getConnection()) {
                break;
            } catch (SQLException e) {
                log.info("Could not establish connection to the database: attempt {}, sleeping for {}ms", i, retryPeriod.toMillis());
                try {
                    Thread.sleep(retryPeriod.toMillis());
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
