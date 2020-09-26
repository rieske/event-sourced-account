package lt.rieske.accounts.eventstore;

import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.util.function.Function;

public class H2 {

    private final DataSource dataSource;

    public static BlobEventStore h2EventStore() {
        if ("mysql".equals(System.getProperty("eventstore"))) {
            return EventStoreFactory.makeEventStore(mysql().dataSource(), Function.identity());
        }
        return EventStoreFactory.makeEventStore(postgres().dataSource(), Function.identity());
    }

    private static H2 mysql() {
        return new H2("MySQL");
    }

    private static H2 postgres() {
        return new H2("PostgreSQL");
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
