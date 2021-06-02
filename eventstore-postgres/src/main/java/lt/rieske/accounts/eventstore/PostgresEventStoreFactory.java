package lt.rieske.accounts.eventstore;

import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.util.function.Function;

public class PostgresEventStoreFactory implements EventStoreFactory{

    @Override
    public BlobEventStore makeEventStore(String jdbcUrl, String username, String password, Function<DataSource, DataSource> initializer) {
        return makeEventStore(postgresDataSource(jdbcUrl, username, password), initializer);
    }

    @Override
    public BlobEventStore makeEventStore(DataSource dataSource, Function<DataSource, DataSource> initializer) {
        DataSourceConfiguration.migrateDatabase(dataSource, "db/migration/postgres");
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
