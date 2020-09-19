package lt.rieske.accounts.api;

import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lt.rieske.accounts.eventsourcing.AggregateNotFoundException;
import lt.rieske.accounts.infrastructure.TracingConfiguration;
import spark.Route;
import spark.Spark;

import java.util.ConcurrentModificationException;

import static spark.Spark.awaitInitialization;
import static spark.Spark.delete;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.path;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.put;


public class Server {

    private final AccountResource accountResource;
    private final PrometheusMeterRegistry meterRegistry;

    private final TracingConfiguration tracingConfiguration;

    Server(AccountResource accountResource, PrometheusMeterRegistry meterRegistry, TracingConfiguration tracingConfiguration) {
        this.accountResource = accountResource;
        this.meterRegistry = meterRegistry;
        this.tracingConfiguration = tracingConfiguration;
    }

    public int start(int port) {
        port(port);

        tracingConfiguration.init();

        path("/api", () -> path("/account/:accountId", () -> {
            post("", metered(accountResource::openAccount, "open_account"));
            get("", metered(accountResource::getAccount, "query_account"));
            get("/events", metered(accountResource::getEvents, "query_account_events"));
            put("/deposit", metered(accountResource::deposit, "deposit"));
            put("/withdraw", metered(accountResource::withdraw, "withdraw"));
            put("/transfer", metered(accountResource::transfer, "transfer"));
            delete("", metered(accountResource::close, "close_account"));
        }));

        get("/ping", (req, res) -> "");

        get("/prometheus", (req, res) -> meterRegistry.scrape());

        exception(IllegalArgumentException.class, tracingConfiguration.exception(accountResource::badRequest));
        exception(AggregateNotFoundException.class, tracingConfiguration.exception(accountResource::notFound));
        exception(ConcurrentModificationException.class, tracingConfiguration.exception(accountResource::conflict));

        awaitInitialization();
        return port();
    }

    public void stop() {
        tracingConfiguration.closeResources();
        Spark.stop();
    }

    private Route metered(Route delegate, String metricTag) {
        var timer = Timer.builder("request_latency").tags("operation", metricTag)
                .publishPercentileHistogram()
                .register(meterRegistry);
        return (request, response) -> timer.recordCallable(() -> delegate.handle(request, response));
    }
}
