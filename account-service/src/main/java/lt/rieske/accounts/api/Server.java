package lt.rieske.accounts.api;

import io.micrometer.observation.Observation;
import lt.rieske.accounts.eventsourcing.AggregateNotFoundException;
import lt.rieske.accounts.infrastructure.ObservabilityConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private final AccountResource accountResource;

    private final ObservabilityConfiguration observabilityConfiguration;

    Server(AccountResource accountResource, ObservabilityConfiguration observabilityConfiguration) {
        this.accountResource = accountResource;
        this.observabilityConfiguration = observabilityConfiguration;
    }

    public int start(int port) {
        port(port);

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

        exception(IllegalArgumentException.class, accountResource::badRequest);
        exception(AggregateNotFoundException.class, accountResource::notFound);
        exception(ConcurrentModificationException.class, accountResource::conflict);

        awaitInitialization();
        return port();
    }

    public void stop() {
        observabilityConfiguration.closeResources();
        Spark.stop();
    }

    private Route metered(Route delegate, String operation) {
        return (request, response) -> {
            log.info("Request {} {}", request.requestMethod(), request.pathInfo());
            Observation observation = observabilityConfiguration.startApiOperationObservation(request, response)
                    .contextualName(operation)
                    .lowCardinalityKeyValue("operation", operation)
                    .lowCardinalityKeyValue("method", request.requestMethod())
                    .lowCardinalityKeyValue("pathTemplate", request.matchedPath())
                    .highCardinalityKeyValue("path", request.pathInfo());
            try (Observation.Scope scope = observation.openScope()) {
                return delegate.handle(request, response);
            } finally {
                observation.stop();
                log.info("Responding with {}", response.status());
            }
        };
    }
}
