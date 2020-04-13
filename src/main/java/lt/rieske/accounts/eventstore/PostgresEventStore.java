package lt.rieske.accounts.eventstore;

import org.postgresql.util.PSQLException;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLTransactionRollbackException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.UUID;


class PostgresEventStore implements BlobEventStore {

    private static final String APPEND_EVENT_SQL =
            "INSERT INTO event_store.Event(aggregateId, sequenceNumber, transactionId, payload) VALUES(?, ?, ?, ?)";
    private static final String SELECT_EVENTS_SQL =
            "SELECT sequenceNumber, transactionId, payload FROM event_store.Event WHERE aggregateId = ? AND sequenceNumber > ? ORDER BY sequenceNumber ASC";

    private static final String REMOVE_SNAPSHOT_SQL =
            "DELETE FROM event_store.Snapshot WHERE aggregateId = ?";
    private static final String STORE_SNAPSHOT_SQL =
            "INSERT INTO event_store.Snapshot(aggregateId, sequenceNumber, payload) VALUES(?, ?, ?)";
    private static final String SELECT_SNAPSHOT_SQL =
            "SELECT sequenceNumber, payload FROM event_store.Snapshot WHERE aggregateId = ?";

    private static final String SELECT_TRANSACTION_SQL =
            "SELECT aggregateId FROM event_store.Event WHERE aggregateId = ? AND transactionId = ?";

    private final DataSource dataSource;

    PostgresEventStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void append(
            Collection<SerializedEvent> serializedEvents,
            Collection<SerializedEvent> serializedSnapshots,
            UUID transactionId) {

        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            insertEvents(connection, serializedEvents);
            updateSnapshots(connection, serializedSnapshots);
            connection.commit();
        } catch (SQLIntegrityConstraintViolationException | SQLTransactionRollbackException e) {
            throw new ConcurrentModificationException(e);
        } catch (PSQLException e) {
            if (e.getMessage().startsWith("ERROR: duplicate key value violates unique constraint")) {
                throw new ConcurrentModificationException(e);
            }
            throw new UncheckedIOException(new IOException(e));
        } catch (SQLException e) {
            throw new UncheckedIOException(new IOException(e));
        }
    }

    @Override
    public List<SerializedEvent> getEvents(UUID aggregateId, long fromVersion) {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(SELECT_EVENTS_SQL)) {
            statement.setObject(1, aggregateId);
            statement.setLong(2, fromVersion);
            try (var resultSet = statement.executeQuery()) {
                List<SerializedEvent> eventPayloads = new ArrayList<>();
                while (resultSet.next()) {
                    eventPayloads.add(new SerializedEvent(aggregateId,
                            resultSet.getLong(1),
                            resultSet.getObject(2, UUID.class),
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
             var statement = connection.prepareStatement(SELECT_SNAPSHOT_SQL)) {
            statement.setObject(1, aggregateId);
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
            statement.setObject(1, aggregateId);
            statement.setObject(2, transactionId);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new UncheckedIOException(new IOException(e));
        }
    }

    private static void insertEvents(Connection connection, Collection<SerializedEvent> events) throws SQLException {
        try (var statement = connection.prepareStatement(APPEND_EVENT_SQL)) {
            for (var e : events) {
                statement.setObject(1, e.aggregateId());
                statement.setLong(2, e.sequenceNumber());
                statement.setObject(3, e.transactionId());
                statement.setBytes(4, e.payload());
                statement.executeUpdate();
            }
        }
    }

    private static void updateSnapshots(Connection connection, Collection<SerializedEvent> events) throws SQLException {
        if (events.isEmpty()) {
            return;
        }
        try (var deleteStatement = connection.prepareStatement(REMOVE_SNAPSHOT_SQL);
             var storeStatement = connection.prepareStatement(STORE_SNAPSHOT_SQL)) {
            for (var e : events) {
                deleteStatement.setObject(1, e.aggregateId());
                deleteStatement.executeUpdate();
                storeStatement.setObject(1, e.aggregateId());
                storeStatement.setLong(2, e.sequenceNumber());
                storeStatement.setBytes(3, e.payload());
                storeStatement.executeUpdate();
            }
        }
    }

}
