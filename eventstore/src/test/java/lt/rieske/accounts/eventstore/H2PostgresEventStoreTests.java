package lt.rieske.accounts.eventstore;

import io.github.rieske.dbtest.extension.DatabaseTestExtension;
import io.github.rieske.dbtest.extension.H2Mode;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Function;

class H2PostgresEventStoreTests extends SqlEventStoreIntegrationTests {

    @RegisterExtension
    public final DatabaseTestExtension database = new H2EventStoreExtension(
            H2Mode.POSTGRESQL,
            dataSource -> EventStoreFactory.postgresEventStore(dataSource, Function.identity())
    );

    @Override
    protected DataSource dataSource() {
        return database.getDataSource();
    }

    @Override
    protected BlobEventStore blobEventStore() {
        return EventStoreFactory.postgresEventStore(dataSource(), Function.identity());
    }

    @Override
    protected void setUUID(PreparedStatement statement, int column, UUID uuid) throws SQLException {
        statement.setObject(column, uuid);
    }
}
