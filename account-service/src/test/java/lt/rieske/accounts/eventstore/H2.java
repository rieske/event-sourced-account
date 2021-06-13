package lt.rieske.accounts.eventstore;

import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.util.function.Function;

public class H2 {

    private final DataSource dataSource;

    public static BlobEventStore eventStore() {
        try {
            Class.forName("org.postgresql.ds.PGSimpleDataSource");
            return EventStoreFactory.postgresEventStore(new H2("PostgreSQL").dataSource, Function.identity());
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.mysql.cj.jdbc.MysqlDataSource");
                return EventStoreFactory.mysqlEventStore(new H2("MySQL").dataSource, Function.identity());
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
}
