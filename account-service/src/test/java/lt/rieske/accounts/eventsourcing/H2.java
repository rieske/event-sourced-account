package lt.rieske.accounts.eventsourcing;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;

public class H2 {

    private static final String DATABASE = "event_store";

    private final DataSource dataSource;

    public static H2 mysql() {
        return new H2("MySQL", "db/mysql");
    }

    public static H2 postgres() {
        return new H2("PostgreSQL", "db/postgresql");
    }

    private H2(String mode, String migrationsLocation) {
        var dataSource = new JdbcDataSource();

        dataSource.setUrl("jdbc:h2:mem:event_store_" + mode + ";MODE=" + mode + ";DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");

        var flyway = Flyway.configure().dataSource(dataSource).locations(migrationsLocation).schemas(DATABASE).load();
        flyway.migrate();

        this.dataSource = dataSource;
    }

    public DataSource dataSource() {
        return dataSource;
    }
}
