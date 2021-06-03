package lt.rieske.accounts.eventstore.mysql;

import com.mysql.cj.jdbc.MysqlDataSource;
import lt.rieske.accounts.eventstore.BlobEventStore;
import lt.rieske.accounts.eventstore.DataSourceConfiguration;
import lt.rieske.accounts.eventstore.EventStoreFactory;

import javax.sql.DataSource;
import java.util.function.Function;

public class MySqlEventStoreFactory implements EventStoreFactory {

    @Override
    public BlobEventStore makeEventStore(String jdbcUrl, String username, String password, Function<DataSource, DataSource> initializer) {
        return makeEventStore(mysqlDataSource(jdbcUrl, username, password), initializer);
    }

    @Override
    public BlobEventStore makeEventStore(DataSource dataSource, Function<DataSource, DataSource> initializer) {
        DataSourceConfiguration.migrateDatabase(dataSource, "db/migration/mysql");
        return new MySqlEventStore(initializer.apply(dataSource));
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
