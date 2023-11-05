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
        super(provider.h2Mode, DatabaseTestExtension.Mode.DATABASE_PER_EXECUTION);
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
    final H2Mode h2Mode;
    final Function<DataSource, BlobEventStore> eventStoreProvider;

    BlobEventStoreProvider() {
        this.h2Mode = getH2Mode();
        this.eventStoreProvider = switch (h2Mode) {
            case POSTGRESQL -> dataSource -> EventStoreFactory.postgresEventStore(dataSource, Function.identity());
            case MYSQL -> dataSource -> EventStoreFactory.mysqlEventStore(dataSource, Function.identity());
        };
    }

    private H2Mode getH2Mode() {
        try {
            Class.forName("org.postgresql.ds.PGSimpleDataSource");
            return H2Mode.POSTGRESQL;
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.mysql.cj.jdbc.MysqlDataSource");
                return H2Mode.MYSQL;
            } catch (ClassNotFoundException classNotFoundException) {
                throw new IllegalStateException("None of supported eventstore drivers found on classpath. This is a build configuration error.");
            }
        }
    }
}
