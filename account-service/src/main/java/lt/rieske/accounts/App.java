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
import java.util.Objects;
import java.util.function.Function;


public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws InterruptedException, SQLException {
        var tracingConfiguration = TracingConfiguration.create(System.getenv("ZIPKIN_URL"));
        var meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        var blobEventStore = eventStore(ds -> pooledMeteredDataSource(tracingConfiguration.decorate(ds), meterRegistry));
        var eventStore = Configuration.accountEventStore(blobEventStore);
        var server = ApiConfiguration.server(eventStore, tracingConfiguration, meterRegistry);
        var port = server.start(8080);
        log.info("Server started on port: {}", port);
    }

    private static BlobEventStore eventStore(Function<DataSource, DataSource> dataSourceInitializer) throws InterruptedException, SQLException {
        return Configuration.blobEventStore(
                getRequiredEnvVariable("JDBC_URL"),
                getRequiredEnvVariable("DB_USER"),
                getRequiredEnvVariable("DB_PASSWORD"),
                dataSourceInitializer);
    }

    private static String getRequiredEnvVariable(String variableName) {
        return Objects.requireNonNull(System.getenv(variableName), String.format("Environment variable '%s' is required", variableName));
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
