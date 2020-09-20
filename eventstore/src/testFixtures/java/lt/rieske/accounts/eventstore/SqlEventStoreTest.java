package lt.rieske.accounts.eventstore;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class SqlEventStoreTest {

    private final DataSource dataSource = dataSource();
    private final BlobEventStore eventStore = blobEventStore();

    protected abstract DataSource dataSource();

    protected abstract BlobEventStore blobEventStore();

    protected abstract void setUUID(PreparedStatement statement, int column, UUID uuid) throws SQLException;

    @Test
    void shouldStoreAnEvent() throws SQLException {
        var aggregateId = UUID.randomUUID();
        eventStore.append(List.of(new SerializedEvent(aggregateId, 42, UUID.randomUUID(), "foobar".getBytes())), List.of(),
                UUID.randomUUID());

        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM event_store.Event WHERE aggregateId=?")) {
            setUUID(statement, 1, aggregateId);
            try (var resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getLong(1)).isEqualTo(1);
            }
        }
    }

    @Test
    void shouldThrowWhenInsertingEventWithExistingSequenceNumberForAggregate() throws SQLException {
        var aggregateId = UUID.randomUUID();
        eventStore.append(List.of(new SerializedEvent(aggregateId, 42, UUID.randomUUID(), "foobar".getBytes())), List.of(),
                UUID.randomUUID());

        assertThatThrownBy(() ->
                eventStore.append(List.of(new SerializedEvent(aggregateId, 42, UUID.randomUUID(), "foobar".getBytes())),
                        List.of(), UUID.randomUUID()))
                .isInstanceOf(ConcurrentModificationException.class);

        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM event_store.Event WHERE aggregateId=?")) {
            setUUID(statement, 1, aggregateId);
            try (var resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getLong(1)).isEqualTo(1);
            }
        }
    }

    @Test
    void shouldGetStoredEvent() {
        var aggregateId = UUID.randomUUID();
        var txId = UUID.randomUUID();
        eventStore.append(List.of(new SerializedEvent(aggregateId, 1, txId, "foobar".getBytes())), List.of(),
                UUID.randomUUID());

        var events = eventStore.getEvents(aggregateId, 0);

        assertThat(events).hasSize(1);
        var event = events.get(0);
        assertThat(event.aggregateId()).isEqualTo(aggregateId);
        assertThat(event.sequenceNumber()).isEqualTo(1);
        assertThat(event.transactionId()).isEqualTo(txId);
        assertThat(event.payload()).isEqualTo("foobar".getBytes());
    }

    @Test
    void shouldGetStoredEventsFromSpecificVersion() {
        var aggregateId = UUID.randomUUID();

        var txId3 = UUID.randomUUID();
        var txId4 = UUID.randomUUID();
        eventStore.append(List.of(
                new SerializedEvent(aggregateId, 1, UUID.randomUUID(), "1".getBytes()),
                new SerializedEvent(aggregateId, 2, UUID.randomUUID(), "2".getBytes()),
                new SerializedEvent(aggregateId, 3, txId3, "3".getBytes()),
                new SerializedEvent(aggregateId, 4, txId4, "4".getBytes())),
                List.of(), UUID.randomUUID());

        var events = eventStore.getEvents(aggregateId, 2);

        assertThat(events).hasSize(2);
        var seq3 = events.get(0);
        assertThat(seq3.aggregateId()).isEqualTo(aggregateId);
        assertThat(seq3.sequenceNumber()).isEqualTo(3);
        assertThat(seq3.transactionId()).isEqualTo(txId3);
        assertThat(seq3.payload()).isEqualTo("3".getBytes());
        var seq4 = events.get(1);
        assertThat(seq4.aggregateId()).isEqualTo(aggregateId);
        assertThat(seq4.sequenceNumber()).isEqualTo(4);
        assertThat(seq4.transactionId()).isEqualTo(txId4);
        assertThat(seq4.payload()).isEqualTo("4".getBytes());
    }

    @Test
    void emptyListWhenNoEventsFound() {
        var events = eventStore.getEvents(UUID.randomUUID(), 2);

        assertThat(events).isEmpty();
    }

    @Test
    void shouldStoreSnapshot() throws SQLException {
        var aggregateId = UUID.randomUUID();

        eventStore.append(List.of(), List.of(new SerializedEvent(aggregateId, 42, UUID.randomUUID(), "foobar".getBytes())),
                UUID.randomUUID());

        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM event_store.Snapshot WHERE aggregateId=?")) {
            setUUID(statement, 1, aggregateId);
            try (var resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getLong(1)).isEqualTo(1);
            }
        }
    }

    @Test
    void shouldLoadLatestSnapshot() {
        var aggregateId = UUID.randomUUID();
        eventStore.append(List.of(), List.of(new SerializedEvent(aggregateId, 50, UUID.randomUUID(), "1".getBytes())),
                UUID.randomUUID());
        eventStore.append(List.of(), List.of(new SerializedEvent(aggregateId, 100, UUID.randomUUID(), "2".getBytes())),
                UUID.randomUUID());
        eventStore.append(List.of(), List.of(new SerializedEvent(aggregateId, 150, UUID.randomUUID(), "3".getBytes())),
                UUID.randomUUID());

        var snapshot = eventStore.loadLatestSnapshot(aggregateId);

        assertThat(snapshot.sequenceNumber()).isEqualTo(150);
        assertThat(snapshot.payload()).isEqualTo("3".getBytes());
    }

    @Test
    void shouldReturnNullWhenSnapshotNotFound() {
        var snapshot = eventStore.loadLatestSnapshot(UUID.randomUUID());

        assertThat(snapshot).isNull();
    }

}
