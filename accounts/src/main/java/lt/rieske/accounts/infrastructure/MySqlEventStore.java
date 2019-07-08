package lt.rieske.accounts.infrastructure;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class MySqlEventStore implements BlobEventStore {

    private static final String APPEND_EVENT_SQL = "INSERT INTO event_store.Event(aggregateId, sequenceNumber, payload) VALUES(?, ?, ?)";
    private static final String SELECT_EVENTS_SQL = "SELECT payload FROM event_store.Event WHERE aggregateId = ? AND sequenceNumber > ?";

    private final DataSource dataSource;

    MySqlEventStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void append(UUID aggregateId, byte[] eventPayload, long sequenceNumber) {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(APPEND_EVENT_SQL)) {
            statement.setBytes(1, uuidToBytes(aggregateId));
            statement.setLong(2, sequenceNumber);
            statement.setBytes(3, eventPayload);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new UncheckedIOException(new IOException(e));
        }
    }

    @Override
    public List<byte[]> getEvents(UUID aggregateId, long fromVersion) {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(SELECT_EVENTS_SQL)) {
            statement.setBytes(1, uuidToBytes(aggregateId));
            statement.setLong(2, fromVersion);
            try (var resultSet = statement.executeQuery()) {
                List<byte[]> eventPayloads = new ArrayList<>();
                while (resultSet.next()) {
                    eventPayloads.add(resultSet.getBytes(1));
                }
                return eventPayloads;
            }
        } catch (SQLException e) {
            throw new UncheckedIOException(new IOException(e));
        }
    }

    @Override
    public void storeSnapshot(UUID aggregateId, long version, byte[] serializedSnapshot) {

    }

    @Override
    public SnapshotBlob loadLatestSnapshot(UUID aggregateId) {
        return null;
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
