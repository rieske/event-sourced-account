package lt.rieske.accounts.eventstore;

import io.github.rieske.dbtest.extension.DatabaseTestExtension;
import io.github.rieske.dbtest.extension.H2FastTestExtension;
import io.github.rieske.dbtest.extension.H2Mode;

import javax.sql.DataSource;
import java.util.function.Function;

public class H2EventStoreExtension extends H2FastTestExtension {
    private final Function<DataSource, BlobEventStore> migrator;

    public H2EventStoreExtension() {
        this(new BlobEventStoreProvider());
    }

    private H2EventStoreExtension(BlobEventStoreProvider provider) {
        super(H2Mode.POSTGRESQL, DatabaseTestExtension.Mode.DATABASE_PER_EXECUTION);
        this.migrator = provider.eventStoreProvider;
    }

    @Override
    protected void migrateDatabase(DataSource dataSource) {
    }

    public BlobEventStore getEventStore() {
        return migrator.apply(getDataSource());
    }
}

class BlobEventStoreProvider {
    final Function<DataSource, BlobEventStore> eventStoreProvider;

    BlobEventStoreProvider() {
        this.eventStoreProvider = dataSource -> EventStoreFactory.postgresEventStore(dataSource, Function.identity());
    }
}
