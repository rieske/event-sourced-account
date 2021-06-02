package lt.rieske.accounts.eventstore.mysql;

import lt.rieske.accounts.eventstore.BlobEventStore;
import lt.rieske.accounts.eventstore.SqlEventStoreIntegrationTests;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Function;

import static lt.rieske.accounts.eventstore.mysql.MySqlEventStore.uuidToBytes;

class H2MySqlEventStoreTests extends SqlEventStoreIntegrationTests {

    private static final JdbcDataSource dataSource = new JdbcDataSource();

    static {
        dataSource.setUrl("jdbc:h2:mem:event_store_MySQL;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    }

    @Override
    protected DataSource dataSource() {
        return dataSource;
    }

    @Override
    protected BlobEventStore blobEventStore() {
        return new MySqlEventStoreFactory().makeEventStore(dataSource(), Function.identity());
    }

    @Override
    protected void setUUID(PreparedStatement statement, int column, UUID uuid) throws SQLException {
        statement.setBytes(column, uuidToBytes(uuid));
    }
}
