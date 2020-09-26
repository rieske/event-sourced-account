package lt.rieske.accounts.eventstore;

import javax.sql.DataSource;
import java.util.function.Function;

interface EventStoreFactory {
    BlobEventStore makeEventStore(String jdbcUrl, String username, String password, Function<DataSource, DataSource> initializer);

    BlobEventStore makeEventStore(DataSource dataSource, Function<DataSource, DataSource> initializer);
}
