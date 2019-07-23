package lt.rieske.accounts;

import lt.rieske.accounts.api.ApiConfiguration;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;

public class App {

    public static void main(String[] args) {
        var dataSource = getDataSource();

        var flyway = Flyway.configure().dataSource(dataSource).schemas("event_store").load();
        flyway.migrate();

        var server = ApiConfiguration.server(dataSource);
        server.start(8080);
    }

    private static DataSource getDataSource() {

        return inMemoryDataSource();
    }

    private static DataSource inMemoryDataSource() {
        var dataSource = new JdbcDataSource();

        dataSource.setUrl("jdbc:h2:mem:event_store;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");

        var flyway = Flyway.configure().dataSource(dataSource).schemas("event_store").load();
        flyway.migrate();

        return dataSource;
    }
}
