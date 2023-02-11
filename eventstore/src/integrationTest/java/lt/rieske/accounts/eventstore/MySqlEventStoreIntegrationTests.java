package lt.rieske.accounts.eventstore;

import io.github.rieske.dbtest.extension.DatabaseTestExtension;
import io.github.rieske.dbtest.extension.MySQLFastTestExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Function;

class MySqlEventStoreIntegrationTests extends SqlEventStoreIntegrationTests {

    static class MysqlEventStoreExtension extends MySQLFastTestExtension {

        public MysqlEventStoreExtension() {
            super("8.0.30", Mode.DATABASE_PER_EXECUTION);
        }

        @Override
        protected void migrateDatabase(DataSource dataSource) {
            EventStoreFactory.mysqlEventStore(dataSource, Function.identity());
        }
    }

    @RegisterExtension
    private final DatabaseTestExtension database = new MysqlEventStoreExtension();

    @Override
    protected DataSource dataSource() {
        return database.getDataSource();
    }

    @Override
    protected BlobEventStore blobEventStore() {
        return new MySqlEventStore(dataSource());
    }

    @Override
    protected void setUUID(PreparedStatement statement, int column, UUID uuid) throws SQLException {
        statement.setBytes(column, MySqlEventStore.uuidToBytes(uuid));
    }
}
