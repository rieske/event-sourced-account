package lt.rieske.accounts;

import com.mysql.cj.jdbc.MysqlDataSource;
import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import lt.rieske.accounts.api.ApiConfiguration;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.time.Duration;

public class App {

    public static void main(String[] args) {
        var dataSource = getDataSource();

        var flyway = Flyway.configure().dataSource(dataSource).schemas("event_store").load();
        flyway.migrate();

        var server = ApiConfiguration.server(dataSource);
        server.start(8080);
    }

    private static DataSource getDataSource() {
        var mysqlUrl = System.getenv("MYSQL_JDBC_URL");
        if (mysqlUrl != null) {
            return mysqlDataSource(mysqlUrl, System.getenv("MYSQL_USER"), System.getenv("MYSQL_PASSWORD"));
        }

        return inMemoryDataSource();
    }

    private static DataSource inMemoryDataSource() {
        var dataSource = new JdbcDataSource();
        dataSource.setUrl("jdbc:h2:mem:event_store;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        return dataSource;
    }

    private static DataSource mysqlDataSource(String jdbcUrl, String username, String password) {
        var dataSource = new MysqlDataSource();
        dataSource.setUrl(jdbcUrl);
        dataSource.setUser(username);
        dataSource.setPassword(password);

        var retryPolicy = new RetryPolicy<>()
                .handle(CommunicationsException.class)
                .withDelay(Duration.ofSeconds(1))
                .withMaxRetries(10);
        Failsafe.with(retryPolicy).run(() -> {
            var c = dataSource.getConnection();
            c.close();
        });

        return dataSource;
    }
}
