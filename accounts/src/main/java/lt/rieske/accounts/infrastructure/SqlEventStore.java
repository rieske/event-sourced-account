package lt.rieske.accounts.infrastructure;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.UUID;


class SqlEventStore implements BlobEventStore {

    private static final String APPEND_EVENT_SQL =
            "INSERT INTO event_store.Event(aggregateId, sequenceNumber, payload) VALUES(?, ?, ?)";
    private static final String SELECT_EVENTS_SQL =
            "SELECT sequenceNumber, payload FROM event_store.Event WHERE aggregateId = ? AND sequenceNumber > ? ORDER BY sequenceNumber ASC";

    private static final String STORE_SNAPSHOT_SQL =
            "INSERT INTO event_store.Snapshot(aggregateId, sequenceNumber, payload) VALUES(?, ?, ?)";
    private static final String SELECT_LATEST_SNAPSHOT_SQL =
            "SELECT sequenceNumber, payload FROM event_store.Snapshot WHERE aggregateId = ? ORDER BY sequenceNumber DESC LIMIT 1";

    private final DataSource dataSource;

    public SqlEventStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void append(List<SerializedEvent> serializedEvents, SerializedEvent serializedSnapshot) {
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            for (var e : serializedEvents) {
                insertPayload(connection, APPEND_EVENT_SQL, e.getAggregateId(), e.getSequenceNumber(), e.getPayload());
            }
            if (serializedSnapshot != null) {
                insertPayload(connection, STORE_SNAPSHOT_SQL, serializedSnapshot.getAggregateId(), serializedSnapshot.getSequenceNumber(), serializedSnapshot.getPayload());
            }
            connection.commit();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ConcurrentModificationException(e);
        } catch (SQLException e) {
            throw new UncheckedIOException(new IOException(e));
        }
    }

    @Override
    public List<SerializedEvent> getEvents(UUID aggregateId, long fromVersion) {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(SELECT_EVENTS_SQL)) {
            statement.setBytes(1, uuidToBytes(aggregateId));
            statement.setLong(2, fromVersion);
            try (var resultSet = statement.executeQuery()) {
                List<SerializedEvent> eventPayloads = new ArrayList<>();
                while (resultSet.next()) {
                    eventPayloads.add(new SerializedEvent(aggregateId, resultSet.getLong(1), resultSet.getBytes(2)));
                }
                return eventPayloads;
            }
        } catch (SQLException e) {
            throw new UncheckedIOException(new IOException(e));
        }
    }

    @Override
    public SerializedEvent loadLatestSnapshot(UUID aggregateId) {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(SELECT_LATEST_SNAPSHOT_SQL)) {
            statement.setBytes(1, uuidToBytes(aggregateId));
            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new SerializedEvent(aggregateId, resultSet.getLong(1), resultSet.getBytes(2));
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new UncheckedIOException(new IOException(e));
        }
    }

    private void insertPayload(Connection connection, String sql, UUID aggregateId, long sequenceNumber, byte[] payload)
            throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            statement.setBytes(1, uuidToBytes(aggregateId));
            statement.setLong(2, sequenceNumber);
            statement.setBytes(3, payload);
            statement.executeUpdate();
        }
    }

    static byte[] uuidToBytes(UUID uuid) {
        byte[] uuidBytes = new byte[16];
        ByteBuffer.wrap(uuidBytes)
                .order(ByteOrder.BIG_ENDIAN)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits());
        return uuidBytes;
    }
}
