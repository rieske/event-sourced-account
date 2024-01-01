package lt.rieske.accounts.api;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import lt.rieske.accounts.domain.Account;
import lt.rieske.accounts.domain.AccountEvent;
import lt.rieske.accounts.domain.AccountSnapshotter;
import lt.rieske.accounts.eventsourcing.AggregateRepository;
import lt.rieske.accounts.eventsourcing.EventStore;
import lt.rieske.accounts.eventstore.BlobEventStore;
import lt.rieske.accounts.eventstore.Configuration;
import lt.rieske.accounts.infrastructure.ObservabilityConfiguration;

public class ApiConfiguration {

    public interface EventStoreSupplier {
        BlobEventStore supply(ObservabilityConfiguration observabilityConfiguration, MeterRegistry meterRegistry);
    }

    public static Server server(EventStoreSupplier eventStoreSupplier, String opentelemetryUrl, String zipkinUrl) {
        var meterRegistry = new OtlpMeterRegistry(key -> switch (key) {
            case "otlp.url" -> opentelemetryUrl;
            case "otlp.baseTimeUnit" -> "SECONDS";
            case "otlp.step" -> "5s";
            case "otlp.resourceAttributes" -> "service.name=" + System.getenv("SERVICE_NAME") + ",service.instance.id=" + System.getenv("HOSTNAME");
            default -> null;
        }, Clock.SYSTEM);
        new ClassLoaderMetrics().bindTo(meterRegistry);
        new JvmMemoryMetrics().bindTo(meterRegistry);
        new JvmGcMetrics().bindTo(meterRegistry);
        new JvmThreadMetrics().bindTo(meterRegistry);
        new ProcessorMetrics().bindTo(meterRegistry);
        new FileDescriptorMetrics().bindTo(meterRegistry);
        new UptimeMetrics().bindTo(meterRegistry);
        new LogbackMetrics().bindTo(meterRegistry);

        var observabilityConfiguration = ObservabilityConfiguration.create(meterRegistry, zipkinUrl);
        var eventStore = Configuration.accountEventStore(eventStoreSupplier.supply(observabilityConfiguration, meterRegistry));
        var accountRepository = snapshottingAccountRepository(eventStore, 50);
        var accountService = new AccountService(accountRepository, eventStore);
        var accountResource = new AccountResource(accountService);

        return new Server(accountResource, observabilityConfiguration);
    }

    public static AggregateRepository<Account, AccountEvent> accountRepository(EventStore<AccountEvent> eventStore) {
        return new AggregateRepository<>(eventStore, Account::new);
    }

    public static AggregateRepository<Account, AccountEvent> snapshottingAccountRepository(
            EventStore<AccountEvent> eventStore, int snapshottingFrequency) {
        return new AggregateRepository<>(eventStore, Account::new, new AccountSnapshotter(snapshottingFrequency));
    }

}
