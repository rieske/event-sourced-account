package lt.rieske.accounts.eventsourcing;

import javax.sql.DataSource;

class H2EventStoreTests extends SqlEventStoreIntegrationTests {

    private static final H2 H2 = new H2();

    @Override
    protected DataSource dataSource() {
        return H2.dataSource();
    }
}
