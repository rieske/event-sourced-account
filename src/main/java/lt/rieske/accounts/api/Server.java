package lt.rieske.accounts.api;

import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import lt.rieske.accounts.eventsourcing.AggregateNotFoundException;
import org.slf4j.MDC;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.util.ConcurrentModificationException;
import java.util.UUID;

import static spark.Spark.afterAfter;
import static spark.Spark.awaitInitialization;
import static spark.Spark.before;
import static spark.Spark.delete;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.path;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.put;


@Slf4j
public class Server {

    private final AccountResource accountResource;
    private final PrometheusMeterRegistry meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    Server(AccountResource accountResource) {
        this.accountResource = accountResource;
        new ClassLoaderMetrics().bindTo(meterRegistry);
        new JvmMemoryMetrics().bindTo(meterRegistry);
        new JvmGcMetrics().bindTo(meterRegistry);
        new ProcessorMetrics().bindTo(meterRegistry);
        new JvmThreadMetrics().bindTo(meterRegistry);
    }

    public int start(int port) {
        port(port);

        path("/api", () -> {
            path("/account/:accountId", () -> {
                post("", accountResource::createAccount);
                get("", accountResource::getAccount);
                get("/events", accountResource::getEvents);
                put("/deposit", accountResource::deposit);
                put("/withdraw", accountResource::withdraw);
                put("/transfer", accountResource::transfer);
                delete("", accountResource::close);
            });
        });

        get("/ping", (req, res) -> "");

        get("/prometheus", (req, res) -> meterRegistry.scrape());

        exception(IllegalArgumentException.class, accountResource::badRequest);
        exception(AggregateNotFoundException.class, accountResource::notFound);
        exception(ConcurrentModificationException.class, accountResource::conflict);

        awaitInitialization();
        return port();
    }

    public void stop() {
        Spark.stop();
    }

}
