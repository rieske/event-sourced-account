package lt.rieske.accounts.eventstore;

import lt.rieske.accounts.eventstore.mysql.MySqlEventStoreFactory;
import lt.rieske.accounts.eventstore.postgres.PostgresEventStoreFactory;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.util.function.Function;

public class H2 {

    private final DataSource dataSource;

    public static BlobEventStore h2EventStore() {
        try {
            Class.forName("org.postgresql.ds.PGSimpleDataSource");
            return new PostgresEventStoreFactory().makeEventStore(new H2("PostgreSQL").dataSource(), Function.identity());
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.mysql.cj.jdbc.MysqlDataSource");
                return new MySqlEventStoreFactory().makeEventStore(new H2("MySQL").dataSource(), Function.identity());
            } catch (ClassNotFoundException classNotFoundException) {
                throw new IllegalStateException("None of supported eventstore drivers found on classpath. This is a build configuration error.");
            }
        }
    }

    private H2(String mode) {
        var dataSource = new JdbcDataSource();
        dataSource.setUrl("jdbc:h2:mem:event_store_" + mode + ";MODE=" + mode + ";DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        this.dataSource = dataSource;
    }

    public DataSource dataSource() {
        return dataSource;
    }
}
