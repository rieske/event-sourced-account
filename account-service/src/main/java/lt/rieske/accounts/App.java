package lt.rieske.accounts;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lt.rieske.accounts.api.ApiConfiguration;
import lt.rieske.accounts.api.TracingConfiguration;
import lt.rieske.accounts.eventstore.BlobEventStore;
import lt.rieske.accounts.eventstore.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.function.Function;


public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    private static final String POSTGRES_JDBC_URL_ENV_VAR = "POSTGRES_JDBC_URL";
    private static final String MYSQL_JDBC_URL_ENV_VAR = "MYSQL_JDBC_URL";

    public static void main(String[] args) throws InterruptedException, SQLException {
        var tracingConfiguration = TracingConfiguration.create(System.getenv("ZIPKIN_URL"));
        var meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        var blobEventStore = eventStore(ds -> pooledMeteredDataSource(tracingConfiguration.decorate(ds), meterRegistry));
        var server = ApiConfiguration.server(blobEventStore, tracingConfiguration, meterRegistry);
        var port = server.start(8080);
        log.info("Server started on port: {}", port);
    }

    private static BlobEventStore eventStore(Function<DataSource, DataSource> dataSourceInitializer) throws InterruptedException, SQLException {
        var postgresUrl = System.getenv(POSTGRES_JDBC_URL_ENV_VAR);
        if (postgresUrl != null) {
            return Configuration.blobEventStore(
                    System.getenv("POSTGRES_JDBC_URL"), System.getenv("POSTGRES_USER"), System.getenv("POSTGRES_PASSWORD"),
                    dataSourceInitializer
            );
        }

        var mysqlUrl = System.getenv(MYSQL_JDBC_URL_ENV_VAR);
        if (mysqlUrl != null) {
            return Configuration.blobEventStore(
                    System.getenv("MYSQL_JDBC_URL"), System.getenv("MYSQL_USER"), System.getenv("MYSQL_PASSWORD"),
                    dataSourceInitializer
            );
        }

        throw new IllegalStateException(String.format("Either %s or %s environment variable has to be specified", POSTGRES_JDBC_URL_ENV_VAR, MYSQL_JDBC_URL_ENV_VAR));
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
