package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.eventstore.BlobEventStore;
import lt.rieske.accounts.eventstore.Configuration;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

class H2PostgresEventStoreTests extends SqlEventStoreIntegrationTests {

    private static final H2 db = H2.postgres();

    @Override
    protected DataSource dataSource() {
        return db.dataSource();
    }

    @Override
    protected BlobEventStore blobEventStore(DataSource dataSource) {
        return Configuration.postgresEventStore(dataSource);
    }

    @Override
    protected void setUUID(PreparedStatement statement, int column, UUID uuid) throws SQLException {
        statement.setObject(column, uuid);
    }
}
