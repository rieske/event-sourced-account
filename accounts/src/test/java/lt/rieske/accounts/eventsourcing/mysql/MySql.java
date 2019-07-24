package lt.rieske.accounts.eventsourcing.mysql;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.flywaydb.core.Flyway;
import org.testcontainers.containers.MySQLContainer;

import javax.sql.DataSource;

class MySql {

    private static final String DATABASE = "event_store";

    private final MySQLContainer mysql = new MySQLContainer().withDatabaseName(DATABASE);

    private final DataSource dataSource;

    MySql() {
        mysql.start();

        var dataSource = new MysqlDataSource();

        dataSource.setUrl(mysql.getJdbcUrl());
        dataSource.setUser(mysql.getUsername());
        dataSource.setPassword(mysql.getPassword());
        dataSource.setDatabaseName(DATABASE);

        this.dataSource = dataSource;

        var flyway = Flyway.configure().dataSource(dataSource).load();
        flyway.migrate();
    }

    void stop() {
        mysql.stop();
    }

    DataSource dataSource() {
        return dataSource;
    }
}
