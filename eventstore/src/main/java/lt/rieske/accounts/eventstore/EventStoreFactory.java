package lt.rieske.accounts.eventstore;

import lt.rieske.accounts.eventstore.mysql.MySqlEventStoreFactory;
import lt.rieske.accounts.eventstore.postgres.PostgresEventStoreFactory;

import javax.sql.DataSource;
import java.util.function.Function;

public interface EventStoreFactory {
    BlobEventStore makeEventStore(String jdbcUrl, String username, String password, Function<DataSource, DataSource> initializer);

    BlobEventStore makeEventStore(DataSource dataSource, Function<DataSource, DataSource> initializer);

    static BlobEventStore makeEventStoreFoo(String jdbcUrl, String username, String password, Function<DataSource, DataSource> initializer) {
        return eventStoreFactory().makeEventStore(jdbcUrl, username, password, initializer);
    }

    private static EventStoreFactory eventStoreFactory() {
        try {
            Class.forName("org.postgresql.ds.PGSimpleDataSource");
            return new PostgresEventStoreFactory();
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.mysql.cj.jdbc.MysqlDataSource");
                return new MySqlEventStoreFactory();
            } catch (ClassNotFoundException classNotFoundException) {
                throw new IllegalStateException("None of supported eventstore drivers found on classpath. This is a build configuration error.");
            }
        }
    }
}
