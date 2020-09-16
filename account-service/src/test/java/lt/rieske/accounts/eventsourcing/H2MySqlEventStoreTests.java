package lt.rieske.accounts.eventsourcing;

import lt.rieske.accounts.eventstore.BlobEventStore;
import lt.rieske.accounts.eventstore.Configuration;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Function;

import static lt.rieske.accounts.eventstore.MySqlEventStore.uuidToBytes;

class H2MySqlEventStoreTests extends SqlEventStoreIntegrationTests {

    private static final H2 db = H2.mysql();

    @Override
    protected DataSource dataSource() {
        return db.dataSource();
    }

    @Override
    protected BlobEventStore blobEventStore() {
        return Configuration.mysqlEventStore(dataSource(), Function.identity());
    }

    @Override
    protected void setUUID(PreparedStatement statement, int column, UUID uuid) throws SQLException {
        statement.setBytes(column, uuidToBytes(uuid));
    }
}
