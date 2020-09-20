package lt.rieske.accounts.eventstore;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Duration;

class DataSourceConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfiguration.class);

    static void migrateDatabase(DataSource dataSource) {
        var flyway = Flyway.configure().dataSource(dataSource).schemas("event_store").load();
        flyway.migrate();
    }

    static void waitForDatabaseToBeAvailable(DataSource dataSource) {
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
