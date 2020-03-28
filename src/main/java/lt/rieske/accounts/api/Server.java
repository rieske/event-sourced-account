package lt.rieske.accounts.api;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import lt.rieske.accounts.eventsourcing.AggregateNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(Server.class);

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
            post("", accountResource::createAccount);
            get("", accountResource::getAccount);
            get("/events", accountResource::getEvents);
            put("/deposit", accountResource::deposit);
            put("/withdraw", accountResource::withdraw);
            put("/transfer", accountResource::transfer);
            delete("", accountResource::close);
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

}
