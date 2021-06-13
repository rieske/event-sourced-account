package lt.rieske.accounts.eventstore;

import com.mysql.cj.jdbc.MysqlDataSource;
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
        try {
            Class.forName("org.postgresql.ds.PGSimpleDataSource");
            return postgresEventStore(jdbcUrl, username, password, initializer);
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.mysql.cj.jdbc.MysqlDataSource");
                return mysqlEventStore(jdbcUrl, username, password, initializer);
            } catch (ClassNotFoundException classNotFoundException) {
                throw new IllegalStateException("None of supported eventstore drivers found on classpath. This is a build configuration error.");
            }
        }
    }

    static BlobEventStore postgresEventStore(String jdbcUrl, String username, String password, Function<DataSource, DataSource> initializer) {
        log.info("Creating PostgreSQL event store");
        return postgresEventStore(postgresDataSource(jdbcUrl, username, password), initializer);
    }

    static BlobEventStore mysqlEventStore(String jdbcUrl, String username, String password, Function<DataSource, DataSource> initializer) {
        log.info("Creating MySQL event store");
        return mysqlEventStore(mysqlDataSource(jdbcUrl, username, password), initializer);
    }

    static BlobEventStore postgresEventStore(DataSource dataSource, Function<DataSource, DataSource> initializer) {
        migrateDatabase(dataSource, "db/migration/postgres");
        return new PostgresEventStore(initializer.apply(dataSource));
    }

    static BlobEventStore mysqlEventStore(DataSource dataSource, Function<DataSource, DataSource> initializer) {
        migrateDatabase(dataSource, "db/migration/mysql");
        return new MySqlEventStore(initializer.apply(dataSource));
    }

    private static DataSource postgresDataSource(String jdbcUrl, String username, String password) {
        var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(jdbcUrl);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    private static DataSource mysqlDataSource(String jdbcUrl, String username, String password) {
        var dataSource = new MysqlDataSource();
        dataSource.setUrl(jdbcUrl);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    private static void migrateDatabase(DataSource dataSource, String migrationResourcesLocation) {
        waitForDatabaseToBeAvailable(dataSource);
        Flyway.configure()
                .dataSource(dataSource)
                .locations(migrationResourcesLocation)
                .schemas("event_store")
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
