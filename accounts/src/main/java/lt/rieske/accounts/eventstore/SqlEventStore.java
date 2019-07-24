package lt.rieske.accounts.eventstore;

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
            "INSERT INTO event_store.Event(aggregateId, sequenceNumber, transactionId, payload) VALUES(?, ?, ?, ?)";
    private static final String SELECT_EVENTS_SQL =
            "SELECT sequenceNumber, transactionId, payload FROM event_store.Event WHERE aggregateId = ? AND sequenceNumber > ? ORDER BY sequenceNumber ASC";

    private static final String APPEND_SNAPSHOT_SQL =
            "INSERT INTO event_store.Snapshot(aggregateId, sequenceNumber, payload) VALUES(?, ?, ?)";
    private static final String SELECT_LATEST_SNAPSHOT_SQL =
            "SELECT sequenceNumber, payload FROM event_store.Snapshot WHERE aggregateId = ? ORDER BY sequenceNumber DESC LIMIT 1";

    private static final String SELECT_TRANSACTION_SQL =
            "SELECT sequenceNumber FROM event_store.Event WHERE aggregateId = ? AND transactionId = ?";

    private final DataSource dataSource;

    SqlEventStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void append(List<SerializedEvent> serializedEvents, SerializedEvent serializedSnapshot) {
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            for (var e : serializedEvents) {
                insertEvent(connection, e);
            }
            if (serializedSnapshot != null) {
                insertSnapshot(connection, serializedSnapshot);
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
                    eventPayloads.add(new SerializedEvent(aggregateId,
                            resultSet.getLong(1),
                            bytesToUUID(resultSet.getBytes(2)),
                            resultSet.getBytes(3)));
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
                    return new SerializedEvent(aggregateId,
                            resultSet.getLong(1),
                            null,
                            resultSet.getBytes(2));
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new UncheckedIOException(new IOException(e));
        }
    }

    @Override
    public boolean transactionExists(UUID aggregateId, UUID transactionId) {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(SELECT_TRANSACTION_SQL)) {
            statement.setBytes(1, uuidToBytes(aggregateId));
            statement.setBytes(2, uuidToBytes(transactionId));
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new UncheckedIOException(new IOException(e));
        }
    }

    private void insertEvent(Connection connection, SerializedEvent event)
            throws SQLException {
        try (var statement = connection.prepareStatement(APPEND_EVENT_SQL)) {
            statement.setBytes(1, uuidToBytes(event.getAggregateId()));
            statement.setLong(2, event.getSequenceNumber());
            statement.setBytes(3, uuidToBytes(event.getTransactionId()));
            statement.setBytes(4, event.getPayload());
            statement.executeUpdate();
        }
    }

    private void insertSnapshot(Connection connection, SerializedEvent event)
            throws SQLException {
        try (var statement = connection.prepareStatement(APPEND_SNAPSHOT_SQL)) {
            statement.setBytes(1, uuidToBytes(event.getAggregateId()));
            statement.setLong(2, event.getSequenceNumber());
            statement.setBytes(3, event.getPayload());
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

    private static UUID bytesToUUID(byte[] bytes) {
        var byteBuffer = ByteBuffer.wrap(bytes)
                .order(ByteOrder.BIG_ENDIAN);
        long high = byteBuffer.getLong();
        long low = byteBuffer.getLong();
        return new UUID(high, low);
    }
}
