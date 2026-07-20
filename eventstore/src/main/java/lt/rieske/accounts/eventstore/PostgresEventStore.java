package lt.rieske.accounts.eventstore;

import org.postgresql.util.PSQLException;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.UUID;


class PostgresEventStore implements BlobEventStore {

    private static final String UNIQUE_CONSTRAINT_VIOLATION_SQLSTATE = "23505";

    private static final String APPEND_EVENT_SQL =
            "INSERT INTO Event(aggregateId, sequenceNumber, transactionId, payload) VALUES(?, ?, ?, ?)";
    private static final String SELECT_EVENTS_SQL =
            "SELECT sequenceNumber, transactionId, payload FROM Event WHERE aggregateId = ? AND sequenceNumber > ? ORDER BY sequenceNumber ASC";

    // Portable upsert (H2 + Postgres): UPDATE then INSERT if missing. Avoids ON CONFLICT (H2).
    private static final String UPDATE_SNAPSHOT_SQL =
            "UPDATE Snapshot SET sequenceNumber = ?, payload = ? WHERE aggregateId = ?";
    private static final String INSERT_SNAPSHOT_SQL =
            "INSERT INTO Snapshot(aggregateId, sequenceNumber, payload) VALUES(?, ?, ?)";
    private static final String SELECT_SNAPSHOT_SQL =
            "SELECT sequenceNumber, payload FROM Snapshot WHERE aggregateId = ?";

    private static final String SELECT_TRANSACTION_SQL =
            "SELECT 1 FROM Event WHERE aggregateId = ? AND transactionId = ? LIMIT 1";

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
            try {
                insertEvents(connection, serializedEvents);
                updateSnapshots(connection, serializedSnapshots);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            if (isUniqueConstraintViolation(e)) {
                throw new ConcurrentModificationException(e);
            }
            throw new UncheckedIOException(new IOException(e));
        }
    }

    private static boolean isUniqueConstraintViolation(SQLException e) {
        for (SQLException current = e; current != null; current = current.getNextException()) {
            if (current instanceof SQLIntegrityConstraintViolationException) {
                return true;
            }
            if (UNIQUE_CONSTRAINT_VIOLATION_SQLSTATE.equals(current.getSQLState())) {
                return true;
            }
            // Batched inserts surface as BatchUpdateException (H2) or PSQLException (Postgres)
            if (current instanceof BatchUpdateException || current instanceof PSQLException) {
                if (UNIQUE_CONSTRAINT_VIOLATION_SQLSTATE.equals(current.getSQLState())) {
                    return true;
                }
            }
        }
        Throwable cause = e.getCause();
        if (cause instanceof SQLException sqlException) {
            return isUniqueConstraintViolation(sqlException);
        }
        return false;
    }

    @Override
    public List<SerializedEvent> getEvents(UUID aggregateId, long fromVersion) {
        try (var connection = dataSource.getConnection()) {
            return getEvents(connection, aggregateId, fromVersion);
        } catch (SQLException e) {
            throw new UncheckedIOException(new IOException(e));
        }
    }

    @Override
    public SerializedEvent loadLatestSnapshot(UUID aggregateId) {
        try (var connection = dataSource.getConnection()) {
            return loadLatestSnapshot(connection, aggregateId);
        } catch (SQLException e) {
            throw new UncheckedIOException(new IOException(e));
        }
    }

    @Override
    public AggregateRead load(UUID aggregateId) {
        try (var connection = dataSource.getConnection()) {
            var snapshot = loadLatestSnapshot(connection, aggregateId);
            long fromVersion = snapshot == null ? 0L : snapshot.sequenceNumber();
            return new AggregateRead(snapshot, getEvents(connection, aggregateId, fromVersion));
        } catch (SQLException e) {
            throw new UncheckedIOException(new IOException(e));
        }
    }

    private static List<SerializedEvent> getEvents(Connection connection, UUID aggregateId, long fromVersion)
            throws SQLException {
        try (var statement = connection.prepareStatement(SELECT_EVENTS_SQL)) {
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
        }
    }

    private static SerializedEvent loadLatestSnapshot(Connection connection, UUID aggregateId) throws SQLException {
        try (var statement = connection.prepareStatement(SELECT_SNAPSHOT_SQL)) {
            statement.setObject(1, aggregateId);
            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new SerializedEvent(aggregateId,
                            resultSet.getLong(1),
                            null,
                            resultSet.getBytes(2));
                }
                return null;
            }
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
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void updateSnapshots(Connection connection, Collection<SerializedEvent> events) throws SQLException {
        if (events.isEmpty()) {
            return;
        }
        try (var update = connection.prepareStatement(UPDATE_SNAPSHOT_SQL);
             var insert = connection.prepareStatement(INSERT_SNAPSHOT_SQL)) {
            for (var e : events) {
                update.setLong(1, e.sequenceNumber());
                update.setBytes(2, e.payload());
                update.setObject(3, e.aggregateId());
                if (update.executeUpdate() == 0) {
                    insert.setObject(1, e.aggregateId());
                    insert.setLong(2, e.sequenceNumber());
                    insert.setBytes(3, e.payload());
                    insert.executeUpdate();
                }
            }
        }
    }

}
