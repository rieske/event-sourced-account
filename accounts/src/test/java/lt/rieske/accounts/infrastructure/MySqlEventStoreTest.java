package lt.rieske.accounts.infrastructure;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.flywaydb.core.Flyway;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.MySQLContainer;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.UUID;

import static lt.rieske.accounts.infrastructure.MySqlEventStore.uuidToBytes;
import static org.assertj.core.api.Assertions.assertThat;

public class MySqlEventStoreTest {

    private static final String DATABASE = "event_store";

    @ClassRule
    public static MySQLContainer mysql = new MySQLContainer().withDatabaseName(DATABASE);

    private DataSource dataSource;

    private BlobEventStore eventStore;

    @Before
    public void prepare() {
        var dataSource = new MysqlDataSource();

        dataSource.setUrl(mysql.getJdbcUrl());
        dataSource.setUser(mysql.getUsername());
        dataSource.setPassword(mysql.getPassword());
        dataSource.setDatabaseName(DATABASE);

        this.dataSource = dataSource;

        var flyway = Flyway.configure().dataSource(dataSource).load();
        flyway.migrate();

        this.eventStore = new MySqlEventStore(dataSource);
    }

    @Test
    public void shouldStoreAnEvent() throws SQLException {
        var aggregateId = UUID.randomUUID();
        eventStore.append(aggregateId, 42, "foobar".getBytes());

        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM event_store.Event WHERE aggregateId=?")) {
            statement.setBytes(1, uuidToBytes(aggregateId));
            try (var resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getLong(1)).isEqualTo(1);
            }
        }
    }

    @Test
    public void shouldGetStoredEvent() {
        var aggregateId = UUID.randomUUID();
        eventStore.append(aggregateId, 1, "foobar".getBytes());

        var events = eventStore.getEvents(aggregateId, 0);

        assertThat(events).containsExactly("foobar".getBytes());
    }

    @Test
    public void shouldGetStoredEventsFromSpecificVersion() {
        var aggregateId = UUID.randomUUID();
        eventStore.append(aggregateId, 1, "1".getBytes());
        eventStore.append(aggregateId, 2, "2".getBytes());
        eventStore.append(aggregateId, 3, "3".getBytes());
        eventStore.append(aggregateId, 4, "4".getBytes());

        var events = eventStore.getEvents(aggregateId, 2);

        assertThat(events).containsExactly("3".getBytes(), "4".getBytes());
    }

    @Test
    public void emptyListWhenNoEventsFound() {
        var events = eventStore.getEvents(UUID.randomUUID(), 2);

        assertThat(events).isEmpty();
    }

    @Test
    public void shouldStoreSnapshot() throws SQLException {
        var aggregateId = UUID.randomUUID();
        eventStore.storeSnapshot(aggregateId, 42, "foobar".getBytes());

        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM event_store.Snapshot WHERE aggregateId=?")) {
            statement.setBytes(1, uuidToBytes(aggregateId));
            try (var resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getLong(1)).isEqualTo(1);
            }
        }
    }

    @Test
    public void shouldLoadLatestSnapshot() {
        var aggregateId = UUID.randomUUID();
        eventStore.storeSnapshot(aggregateId, 50, "1".getBytes());
        eventStore.storeSnapshot(aggregateId, 100, "2".getBytes());
        eventStore.storeSnapshot(aggregateId, 150, "3".getBytes());

        var snapshot = eventStore.loadLatestSnapshot(aggregateId);

        assertThat(snapshot.getVersion()).isEqualTo(150);
        assertThat(snapshot.getSnapshotEvent()).isEqualTo("3".getBytes());
    }

    @Test
    public void shouldReturnNullWhenSnapshotNotFound() {
        var snapshot = eventStore.loadLatestSnapshot(UUID.randomUUID());

        assertThat(snapshot).isNull();
    }

}
