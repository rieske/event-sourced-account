package lt.rieske.accounts.infrastructure;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.flywaydb.core.Flyway;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.MySQLContainer;

import javax.sql.DataSource;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class MySqlEventStoreTest {

    private static final String DATABASE = "event_store";

    @Rule
    public MySQLContainer mysql = new MySQLContainer().withDatabaseName(DATABASE);

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
        eventStore.append(aggregateId, "foobar".getBytes(), 42);

        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("SELECT COUNT(*) FROM event_store.Event WHERE aggregateId=?")) {
            statement.setBytes(1, uuidToBytes(aggregateId));
            try (var resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getLong(1)).isEqualTo(1);
            }
        }
    }

    @Test
    public void shouldGetStoredEvents() {
        var aggregateId = UUID.randomUUID();
        eventStore.append(aggregateId, "foobar".getBytes(), 42);

        var events = eventStore.getEvents(aggregateId, 0);

        assertThat(events).hasSize(1);
        assertThat(events).containsExactly("foobar".getBytes());
    }

    private static byte[] uuidToBytes(UUID uuid) {
        byte[] uuidBytes = new byte[16];
        ByteBuffer.wrap(uuidBytes)
                .order(ByteOrder.BIG_ENDIAN)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits());
        return uuidBytes;
    }
}
