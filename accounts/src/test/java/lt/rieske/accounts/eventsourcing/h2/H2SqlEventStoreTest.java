package lt.rieske.accounts.eventsourcing.h2;

import lt.rieske.accounts.infrastructure.SqlEventStoreTest;

import javax.sql.DataSource;

class H2SqlEventStoreTest extends SqlEventStoreTest {

    private static final H2 H2 = new H2();

    @Override
    protected DataSource dataSource() {
        return H2.dataSource();
    }

}
