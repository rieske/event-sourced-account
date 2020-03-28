package lt.rieske.accounts;

import com.mysql.cj.jdbc.MysqlDataSource;
import lt.rieske.accounts.api.ApiConfiguration;
import lt.rieske.accounts.api.TracingConfiguration;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.time.Duration;


public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws InterruptedException {
        var dataSource = createDataSource();
        migrateDatabase(dataSource);

        var tracingConfiguration = TracingConfiguration.create(System.getenv("ZIPKIN_URL"));
        var server = ApiConfiguration.server(dataSource, tracingConfiguration);
        var port = server.start(8080);
        log.info("Server started on port: {}", port);
    }

    private static void migrateDatabase(DataSource dataSource) {
        var flyway = Flyway.configure().dataSource(dataSource).schemas("event_store").load();
        flyway.migrate();
    }

    private static DataSource createDataSource() throws InterruptedException {
        var mysqlUrl = System.getenv("MYSQL_JDBC_URL");
        if (mysqlUrl != null) {
            return mysqlDataSource(mysqlUrl, System.getenv("MYSQL_USER"), System.getenv("MYSQL_PASSWORD"));
        }

        return inMemoryDataSource();
    }

    private static DataSource inMemoryDataSource() {
        var dataSource = new JdbcDataSource();
        dataSource.setUrl("jdbc:h2:mem:event_store;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        return dataSource;
    }

    private static DataSource mysqlDataSource(String jdbcUrl, String username, String password) throws InterruptedException {
        var dataSource = new MysqlDataSource();
        dataSource.setUrl(jdbcUrl);
        dataSource.setUser(username);
        dataSource.setPassword(password);

        waitForDatabaseToBeAvailable(dataSource, 20, Duration.ofSeconds(1));

        return dataSource;
    }

    private static void waitForDatabaseToBeAvailable(DataSource dataSource, int maxRetries, Duration retryPeriod) throws InterruptedException {
        for (int i = 0; i < maxRetries; i++) {
            try (var conn = dataSource.getConnection()) {
                break;
            } catch (SQLRecoverableException e) {
                log.info("Could not establish connection to the database: attempt {}, sleeping for {}ms", i, retryPeriod.toMillis());
                Thread.sleep(retryPeriod.toMillis());
            } catch (SQLException e) {
                throw new IllegalStateException("Can not connect to the database, aborting", e);
            }
        }
    }
}
