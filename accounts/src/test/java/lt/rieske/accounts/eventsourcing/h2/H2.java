package lt.rieske.accounts.eventsourcing.h2;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;

public class H2 {

    private static final String DATABASE = "event_store";

    private final DataSource dataSource;

    public H2() {
        var dataSource = new JdbcDataSource();

        dataSource.setUrl("jdbc:h2:mem:event_store;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");

        var flyway = Flyway.configure().dataSource(dataSource).schemas(DATABASE).load();
        flyway.migrate();

        this.dataSource = dataSource;
    }

    public DataSource dataSource() {
        return dataSource;
    }
}
