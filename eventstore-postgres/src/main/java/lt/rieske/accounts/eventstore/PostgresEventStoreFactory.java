package lt.rieske.accounts.eventstore;

import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.util.function.Function;

public class PostgresEventStoreFactory {
    static BlobEventStore postgresEventStore(String jdbcUrl, String username, String password, Function<DataSource, DataSource> initializer) {
        return postgresEventStore(postgresDataSource(jdbcUrl, username, password), initializer);
    }

    public static BlobEventStore postgresEventStore(DataSource dataSource, Function<DataSource, DataSource> initializer) {
        DataSourceConfiguration.migrateDatabase(dataSource, "db/postgresql");
        return new PostgresEventStore(initializer.apply(dataSource));
    }

    private static DataSource postgresDataSource(String jdbcUrl, String username, String password) {
        var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(jdbcUrl);
        dataSource.setUser(username);
        dataSource.setPassword(password);

        DataSourceConfiguration.waitForDatabaseToBeAvailable(dataSource);

        return dataSource;
    }
}
