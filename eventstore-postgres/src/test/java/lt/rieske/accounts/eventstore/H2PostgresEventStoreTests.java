package lt.rieske.accounts.eventstore;

import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Function;

class H2PostgresEventStoreTests extends SqlEventStoreIntegrationTests {

    private static final JdbcDataSource dataSource = new JdbcDataSource();

    static {
        dataSource.setUrl("jdbc:h2:mem:event_store_PostgreSQL;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    }

    @Override
    protected DataSource dataSource() {
        return dataSource;
    }

    @Override
    protected BlobEventStore blobEventStore() {
        return PostgresEventStoreFactory.postgresEventStore(dataSource(), Function.identity());
    }

    @Override
    protected void setUUID(PreparedStatement statement, int column, UUID uuid) throws SQLException {
        statement.setObject(column, uuid);
    }
}
