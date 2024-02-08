package lt.rieske.accounts.eventstore;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lt.rieske.accounts.domain.AccountEvent;
import lt.rieske.accounts.eventsourcing.EventStore;

import javax.sql.DataSource;

public class Configuration {

    public static EventStore<AccountEvent> accountEventStore(BlobEventStore blobEventStore) {
        return new SerializingEventStore<>(new MessagePackAccountEventSerializer(), blobEventStore);
    }

    public static BlobEventStore blobEventStore(String jdbcUrl, String username, String password) {
        return EventStoreFactory.makeEventStore(jdbcUrl, username, password, Configuration::pooledDataSource);
    }

    private static DataSource pooledDataSource(DataSource dataSource) {
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
        return new HikariDataSource(config);
    }

    private Configuration() {
    }
}
