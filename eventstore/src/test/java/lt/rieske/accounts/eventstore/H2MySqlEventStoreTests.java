package lt.rieske.accounts.eventstore;

import io.github.rieske.dbtest.extension.DatabaseTestExtension;
import io.github.rieske.dbtest.extension.H2Mode;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Function;

import static lt.rieske.accounts.eventstore.MySqlEventStore.uuidToBytes;

class H2MySqlEventStoreTests extends SqlEventStoreIntegrationTests {

    @RegisterExtension
    public final DatabaseTestExtension database = new H2EventStoreExtension(
            H2Mode.MYSQL,
            dataSource -> EventStoreFactory.mysqlEventStore(dataSource, Function.identity())
    );

    @Override
    protected DataSource dataSource() {
        return database.getDataSource();
    }

    @Override
    protected BlobEventStore blobEventStore() {
        return EventStoreFactory.mysqlEventStore(dataSource(), Function.identity());
    }

    @Override
    protected void setUUID(PreparedStatement statement, int column, UUID uuid) throws SQLException {
        statement.setBytes(column, uuidToBytes(uuid));
    }
}
