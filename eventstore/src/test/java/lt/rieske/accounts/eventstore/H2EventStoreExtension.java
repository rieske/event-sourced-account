package lt.rieske.accounts.eventstore;

import io.github.rieske.dbtest.extension.DatabaseTestExtension;
import io.github.rieske.dbtest.extension.H2FastTestExtension;
import io.github.rieske.dbtest.extension.H2Mode;

import javax.sql.DataSource;
import java.util.function.Consumer;

class H2EventStoreExtension extends H2FastTestExtension {
    private final Consumer<DataSource> migrator;

    H2EventStoreExtension(H2Mode h2Mode, Consumer<DataSource> migrator) {
        super(h2Mode, DatabaseTestExtension.Mode.DATABASE_PER_EXECUTION);
        this.migrator = migrator;
    }

    @Override
    protected void migrateDatabase(DataSource dataSource) {
        migrator.accept(dataSource);
    }
}
