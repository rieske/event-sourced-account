package lt.rieske.accounts.eventstore;

import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;

public class H2 {

    private final DataSource dataSource;

    public static H2 mysql() {
        return new H2("MySQL");
    }

    public static H2 postgres() {
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
