package lt.rieske.accounts.eventstore;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.util.function.Function;

public final class EventStoreFactory {

    private EventStoreFactory() {
    }

    public static BlobEventStore makeEventStore(String jdbcUrl, String username, String password, Function<DataSource, DataSource> initializer) {
        try {
            Class.forName("org.postgresql.ds.PGSimpleDataSource");
            var dataSource = postgresDataSource(jdbcUrl, username, password);
            return postgresEventStore(dataSource, initializer);
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.mysql.cj.jdbc.MysqlDataSource");
                var dataSource = mysqlDataSource(jdbcUrl, username, password);
                return mysqlEventStore(dataSource, initializer);
            } catch (ClassNotFoundException classNotFoundException) {
                throw new IllegalStateException("None of supported eventstore drivers found on classpath. This is a build configuration error.");
            }
        }
    }

    static BlobEventStore postgresEventStore(String jdbcUrl, String username, String password, Function<DataSource, DataSource> initializer) {
        var dataSource = postgresDataSource(jdbcUrl, username, password);
        DataSourceConfiguration.migrateDatabase(dataSource, "db/migration/postgres");
        return new PostgresEventStore(initializer.apply(dataSource));
    }

    static BlobEventStore mysqlEventStore(String jdbcUrl, String username, String password, Function<DataSource, DataSource> initializer) {
        var dataSource = mysqlDataSource(jdbcUrl, username, password);
        DataSourceConfiguration.migrateDatabase(dataSource, "db/migration/mysql");
        return new MySqlEventStore(initializer.apply(dataSource));
    }

    static BlobEventStore postgresEventStore(DataSource dataSource, Function<DataSource, DataSource> initializer) {
        DataSourceConfiguration.migrateDatabase(dataSource, "db/migration/postgres");
        return new PostgresEventStore(initializer.apply(dataSource));
    }

    static BlobEventStore mysqlEventStore(DataSource dataSource, Function<DataSource, DataSource> initializer) {
        DataSourceConfiguration.migrateDatabase(dataSource, "db/migration/mysql");
        return new MySqlEventStore(initializer.apply(dataSource));
    }

    private static DataSource postgresDataSource(String jdbcUrl, String username, String password) {
        var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(jdbcUrl);
        dataSource.setUser(username);
        dataSource.setPassword(password);

        DataSourceConfiguration.waitForDatabaseToBeAvailable(dataSource);

        return dataSource;
    }

    private static DataSource mysqlDataSource(String jdbcUrl, String username, String password) {
        var dataSource = new MysqlDataSource();
        dataSource.setUrl(jdbcUrl);
        dataSource.setUser(username);
        dataSource.setPassword(password);

        DataSourceConfiguration.waitForDatabaseToBeAvailable(dataSource);

        return dataSource;
    }
}
