package lt.rieske.accounts.eventstore;

import com.mysql.cj.jdbc.MysqlDataSource;

import javax.sql.DataSource;
import java.util.function.Function;

public class EventStoreFactory {

    public static BlobEventStore makeEventStore(String jdbcUrl, String username, String password, Function<DataSource, DataSource> initializer) {
        return makeEventStore(mysqlDataSource(jdbcUrl, username, password), initializer);
    }

    static BlobEventStore makeEventStore(DataSource dataSource, Function<DataSource, DataSource> initializer) {
        DataSourceConfiguration.migrateDatabase(dataSource);
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
