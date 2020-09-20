package lt.rieske.accounts.eventstore;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import lt.rieske.accounts.domain.AccountEventsVisitor;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.infrastructure.TracingConfiguration;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

public class Configuration {

    public static EventStore<AccountEventsVisitor> accountEventStore(BlobEventStore blobEventStore) {
        return new SerializingEventStore<>(new MessagePackAccountEventSerializer(), blobEventStore);
    }

    public static BlobEventStore blobEventStore(String jdbcUrl, String username, String password,
                                                TracingConfiguration tracingConfiguration, MeterRegistry meterRegistry) {
        return blobEventStore(jdbcUrl, username, password, ds -> pooledMeteredDataSource(tracingConfiguration.decorate(ds), meterRegistry));
    }

    private static BlobEventStore blobEventStore(String jdbcUrl, String username, String password,
                                         Function<DataSource, DataSource> initializer) {
        try {
            if (jdbcUrl.startsWith("jdbc:postgresql://")) {
                Class<?> eventstoreFactoryClass = Class.forName("lt.rieske.accounts.eventstore.EventStoreFactory");
                Method initMethod = eventstoreFactoryClass.getMethod("makeEventStore", String.class, String.class, String.class, Function.class);
                return (BlobEventStore) initMethod.invoke(eventstoreFactoryClass, jdbcUrl, username, password, initializer);
            } else if (jdbcUrl.startsWith("jdbc:mysql://")) {
                Class<?> eventstoreFactoryClass = Class.forName("lt.rieske.accounts.eventstore.EventStoreFactory");
                Method initMethod = eventstoreFactoryClass.getMethod("makeEventStore", String.class, String.class, String.class, Function.class);
                return (BlobEventStore) initMethod.invoke(eventstoreFactoryClass, jdbcUrl, username, password, initializer);
            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
        throw new IllegalStateException(String.format("Unsupported JDBC URL '%s'", jdbcUrl));
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
