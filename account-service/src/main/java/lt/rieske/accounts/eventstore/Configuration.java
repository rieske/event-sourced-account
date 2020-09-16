package lt.rieske.accounts.eventstore;

import com.mysql.cj.jdbc.MysqlDataSource;
import lt.rieske.accounts.domain.AccountEventsVisitor;
import lt.rieske.accounts.eventsourcing.EventStore;
import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.time.Duration;
import java.util.function.Function;


public class Configuration {

    private static final Logger log = LoggerFactory.getLogger(Configuration.class);

    public static EventStore<AccountEventsVisitor> accountEventStore(BlobEventStore blobEventStore) {
        return new SerializingEventStore<>(new MessagePackAccountEventSerializer(), blobEventStore);
    }

    public static BlobEventStore blobEventStore(String jdbcUrl, String username, String password,
                                                Function<DataSource, DataSource> initializer) throws SQLException, InterruptedException {
        if (jdbcUrl.startsWith("jdbc:postgresql://")) {
            return postgresEventStore(postgresDataSource(jdbcUrl, username, password), initializer);
        } else if (jdbcUrl.startsWith("jdbc:mysql://")) {
            return mysqlEventStore(mysqlDataSource(jdbcUrl, username, password), initializer);
        }
        throw new IllegalStateException(String.format("Unsupported JDBC URL '%s'", jdbcUrl));
    }

    public static BlobEventStore postgresEventStore(DataSource dataSource, Function<DataSource, DataSource> initializer) {
        migrateDatabase(dataSource, "db/postgresql");
        return new PostgresEventStore(initializer.apply(dataSource));
    }

    static BlobEventStore mysqlEventStore(DataSource dataSource, Function<DataSource, DataSource> initializer) {
        migrateDatabase(dataSource, "db/mysql");
        return new MySqlEventStore(initializer.apply(dataSource));
    }

    private static DataSource postgresDataSource(String jdbcUrl, String username, String password) throws InterruptedException, SQLException {
        var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(jdbcUrl);
        dataSource.setUser(username);
        dataSource.setPassword(password);

        waitForDatabaseToBeAvailable(dataSource);

        return dataSource;
    }

    private static DataSource mysqlDataSource(String jdbcUrl, String username, String password) throws InterruptedException, SQLException {
        var dataSource = new MysqlDataSource();
        dataSource.setUrl(jdbcUrl);
        dataSource.setUser(username);
        dataSource.setPassword(password);

        waitForDatabaseToBeAvailable(dataSource);

        return dataSource;
    }

    private static void migrateDatabase(DataSource dataSource, String schemaLocation) {
        var flyway = Flyway.configure().dataSource(dataSource).locations(schemaLocation).schemas("event_store").load();
        flyway.migrate();
    }

    private static void waitForDatabaseToBeAvailable(DataSource dataSource) throws InterruptedException, SQLException {
        var retryPeriod = Duration.ofSeconds(1);
        for (int i = 0; i < 20; i++) {
            try (var conn = dataSource.getConnection()) {
                break;
            } catch (PSQLException | SQLRecoverableException e) {
                log.info("Could not establish connection to the database: attempt {}, sleeping for {}ms", i, retryPeriod.toMillis());
                Thread.sleep(retryPeriod.toMillis());
            }
        }
    }
}
