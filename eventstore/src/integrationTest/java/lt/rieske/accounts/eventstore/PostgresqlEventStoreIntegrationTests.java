package lt.rieske.accounts.eventstore;

import io.github.rieske.dbtest.extension.DatabaseTestExtension;
import io.github.rieske.dbtest.extension.PostgreSQLFastTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Function;

class PostgresqlEventStoreIntegrationTests extends SqlEventStoreIntegrationTests {

    static class PostgresEventStoreExtension extends PostgreSQLFastTestExtension {

        public PostgresEventStoreExtension() {
            super("14.4", Mode.DATABASE_PER_TEST_METHOD);
        }

        @Override
        protected void migrateDatabase(DataSource dataSource) {
            EventStoreFactory.postgresEventStore(dataSource, Function.identity());
        }
    }

    @RegisterExtension
    private final DatabaseTestExtension database = new PostgresEventStoreExtension();

    @Override
    protected DataSource dataSource() {
        return database.getDataSource();
    }

    @Override
    protected BlobEventStore blobEventStore() {
        return new PostgresEventStore(dataSource());
    }

    @Override
    protected void setUUID(PreparedStatement statement, int column, UUID uuid) throws SQLException {
        statement.setObject(column, uuid);
    }
}
