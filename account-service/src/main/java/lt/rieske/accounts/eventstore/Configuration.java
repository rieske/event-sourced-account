package lt.rieske.accounts.eventstore;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import lt.rieske.accounts.domain.AccountEventsVisitor;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.infrastructure.TracingConfiguration;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;

public class Configuration {

    public static EventStore<AccountEventsVisitor> accountEventStore(BlobEventStore blobEventStore) {
        return new SerializingEventStore<>(new MessagePackAccountEventSerializer(), blobEventStore);
    }

    public static BlobEventStore blobEventStore(String jdbcUrl, String username, String password,
                                                TracingConfiguration tracingConfiguration, MeterRegistry meterRegistry) {
        return instantiateEventStoreFactory().makeEventStore(
                jdbcUrl, username, password, ds -> pooledMeteredDataSource(tracingConfiguration.decorate(ds), meterRegistry));
    }

    static EventStoreFactory instantiateEventStoreFactory() {
        var eventStoreFactoryClass = eventStoreFactoryClass();
        try {
            return eventStoreFactoryClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Class<? extends EventStoreFactory> eventStoreFactoryClass() {
        try {
            return Class.forName("lt.rieske.accounts.eventstore.PostgresEventStoreFactory").asSubclass(EventStoreFactory.class);
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName("lt.rieske.accounts.eventstore.MySqlEventStoreFactory").asSubclass(EventStoreFactory.class);
            } catch (ClassNotFoundException classNotFoundException) {
                throw new IllegalStateException("None of supported eventstores found on classpath. This is a build configuration error.");
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
