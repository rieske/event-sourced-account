package lt.rieske.accounts.infrastructure;

import org.junit.AfterClass;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static lt.rieske.accounts.infrastructure.MySqlEventStore.uuidToBytes;
import static org.assertj.core.api.Assertions.assertThat;

public class MySqlEventStoreTest {

    private static final MySql MYSQL = new MySql();

    private final DataSource dataSource = MYSQL.dataSource();
    private final SqlEventStore eventStore = new MySqlEventStore(MYSQL.dataSource());

    @AfterClass
    public static void stopDatabase() {
        MYSQL.stop();
    }

    @Test
    public void shouldStoreAnEvent() throws SQLException {
        var aggregateId = UUID.randomUUID();
        eventStore.append(List.of(new SerializedEvent(aggregateId, 42, "foobar".getBytes())), null);

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
        eventStore.append(List.of(new SerializedEvent(aggregateId, 1, "foobar".getBytes())), null);

        var events = eventStore.getEvents(aggregateId, 0);

        assertThat(events).containsExactly(new SerializedEvent(aggregateId, 1, "foobar".getBytes()));
    }

    @Test
    public void shouldGetStoredEventsFromSpecificVersion() {
        var aggregateId = UUID.randomUUID();

        eventStore.append(List.of(
                new SerializedEvent(aggregateId, 1, "1".getBytes()),
                new SerializedEvent(aggregateId, 2, "2".getBytes()),
                new SerializedEvent(aggregateId, 3, "3".getBytes()),
                new SerializedEvent(aggregateId, 4, "4".getBytes())),
                null);

        var events = eventStore.getEvents(aggregateId, 2);

        assertThat(events).containsExactly(
                new SerializedEvent(aggregateId, 3, "3".getBytes()),
                new SerializedEvent(aggregateId, 4, "4".getBytes()));
    }

    @Test
    public void emptyListWhenNoEventsFound() {
        var events = eventStore.getEvents(UUID.randomUUID(), 2);

        assertThat(events).isEmpty();
    }

    @Test
    public void shouldStoreSnapshot() throws SQLException {
        var aggregateId = UUID.randomUUID();

        eventStore.append(List.of(), new SerializedEvent(aggregateId, 42, "foobar".getBytes()));

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
        eventStore.append(List.of(), new SerializedEvent(aggregateId, 50, "1".getBytes()));
        eventStore.append(List.of(), new SerializedEvent(aggregateId, 100, "2".getBytes()));
        eventStore.append(List.of(), new SerializedEvent(aggregateId, 150, "3".getBytes()));

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
