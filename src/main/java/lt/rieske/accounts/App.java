package lt.rieske.accounts;

import com.mysql.cj.jdbc.MysqlDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lt.rieske.accounts.api.ApiConfiguration;
import lt.rieske.accounts.api.TracingConfiguration;
import lt.rieske.accounts.eventstore.BlobEventStore;
import lt.rieske.accounts.eventstore.Configuration;
import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.time.Duration;


public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    private static final String POSTGRES_JDBC_URL_ENV_VAR = "POSTGRES_JDBC_URL";
    private static final String MYSQL_JDBC_URL_ENV_VAR = "MYSQL_JDBC_URL";

    public static void main(String[] args) throws InterruptedException {
        var tracingConfiguration = TracingConfiguration.create(System.getenv("ZIPKIN_URL"));
        var meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        var blobEventStore = eventStore(tracingConfiguration, meterRegistry);
        var server = ApiConfiguration.server(blobEventStore, tracingConfiguration, meterRegistry);
        var port = server.start(8080);
        log.info("Server started on port: {}", port);
    }

    private static BlobEventStore eventStore(TracingConfiguration tracingConfiguration, PrometheusMeterRegistry meterRegistry) throws InterruptedException {
        var postgresUrl = System.getenv(POSTGRES_JDBC_URL_ENV_VAR);
        if (postgresUrl != null) {
            DataSource dataSource = postgresDataSource(postgresUrl, System.getenv("POSTGRES_USER"), System.getenv("POSTGRES_PASSWORD"));
            return Configuration.postgresEventStore(pooledMeteredDataSource(tracingConfiguration.decorate(dataSource), meterRegistry));
        }

        var mysqlUrl = System.getenv(MYSQL_JDBC_URL_ENV_VAR);
        if (mysqlUrl != null) {
            DataSource dataSource = mysqlDataSource(mysqlUrl, System.getenv("MYSQL_USER"), System.getenv("MYSQL_PASSWORD"));
            return Configuration.mysqlEventStore(pooledMeteredDataSource(tracingConfiguration.decorate(dataSource), meterRegistry));
        }

        throw new IllegalStateException(String.format("Either %s or %s environment variable has to be specified", POSTGRES_JDBC_URL_ENV_VAR, MYSQL_JDBC_URL_ENV_VAR));
    }

    private static void migrateDatabase(DataSource dataSource, String schemaLocation) {
        var flyway = Flyway.configure().dataSource(dataSource).locations(schemaLocation).schemas("event_store").load();
        flyway.migrate();
    }

    private static DataSource mysqlDataSource(String jdbcUrl, String username, String password) throws InterruptedException {
        var dataSource = new MysqlDataSource();
        dataSource.setUrl(jdbcUrl);
        dataSource.setUser(username);
        dataSource.setPassword(password);

        waitForDatabaseToBeAvailable(dataSource);
        migrateDatabase(dataSource, "db/mysql");

        return dataSource;
    }

    private static DataSource postgresDataSource(String jdbcUrl, String username, String password) throws InterruptedException {
        var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(jdbcUrl);
        dataSource.setUser(username);
        dataSource.setPassword(password);

        waitForDatabaseToBeAvailable(dataSource);
        migrateDatabase(dataSource, "db/postgresql");

        return dataSource;
    }

    private static void waitForDatabaseToBeAvailable(DataSource dataSource) throws InterruptedException {
        var retryPeriod = Duration.ofSeconds(1);
        for (int i = 0; i < 20; i++) {
            try (var conn = dataSource.getConnection()) {
                break;
            } catch (PSQLException | SQLRecoverableException e) {
                log.info("Could not establish connection to the database: attempt {}, sleeping for {}ms", i, retryPeriod.toMillis());
                Thread.sleep(retryPeriod.toMillis());
            } catch (SQLException e) {
                throw new IllegalStateException("Can not connect to the database, aborting", e);
            }
        }
    }

    private static DataSource pooledMeteredDataSource(DataSource dataSource, MeterRegistry meterRegistry) {
        var config = new HikariConfig();
        config.setPoolName("eventStore");
        config.setMaximumPoolSize(5);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setDataSource(dataSource);
        config.setMetricRegistry(meterRegistry);
        return new HikariDataSource(config);
    }
}
