package lt.rieske.accounts.api;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.domain.AccountEventsVisitor;
import lt.rieske.accounts.domain.AccountSnapshotter;
import lt.rieske.accounts.eventsourcing.AggregateRepository;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventstore.BlobEventStore;
import lt.rieske.accounts.eventstore.Configuration;
import lt.rieske.accounts.infrastructure.TracingConfiguration;

public class ApiConfiguration {

    public interface EventStoreSupplier {
        BlobEventStore supply(TracingConfiguration tracingConfiguration, MeterRegistry meterRegistry);
    }

    public static Server server(EventStoreSupplier eventStoreSupplier, String zipkinUrl) {
        var meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        new ClassLoaderMetrics().bindTo(meterRegistry);
        new JvmMemoryMetrics().bindTo(meterRegistry);
        new JvmGcMetrics().bindTo(meterRegistry);
        new JvmThreadMetrics().bindTo(meterRegistry);
        new ProcessorMetrics().bindTo(meterRegistry);
        new FileDescriptorMetrics().bindTo(meterRegistry);
        new UptimeMetrics().bindTo(meterRegistry);
        new LogbackMetrics().bindTo(meterRegistry);

        var tracingConfiguration = TracingConfiguration.create(zipkinUrl);
        var eventStore = Configuration.accountEventStore(eventStoreSupplier.supply(tracingConfiguration, meterRegistry));
        var accountRepository = snapshottingAccountRepository(eventStore, 50);
        var accountService = new AccountService(accountRepository, eventStore);
        var accountResource = new AccountResource(accountService);

        return new Server(accountResource, meterRegistry, tracingConfiguration);
    }

    public static AggregateRepository<Account, AccountEventsVisitor> accountRepository(EventStore<AccountEventsVisitor> eventStore) {
        return new AggregateRepository<>(eventStore, Account::new);
    }

    public static AggregateRepository<Account, AccountEventsVisitor> snapshottingAccountRepository(
            EventStore<AccountEventsVisitor> eventStore, int snapshottingFrequency) {
        return new AggregateRepository<>(eventStore, Account::new, new AccountSnapshotter(snapshottingFrequency));
    }

}
