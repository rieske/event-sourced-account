package lt.rieske.accounts.api;

import lt.rieske.accounts.eventsourcing.AggregateNotFoundException;
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

    Server(AccountResource accountResource) {
        this.accountResource = accountResource;
    }

    public int start(int port) {
        port(port);

        path("/api", () -> path("/account/:accountId", () -> {
            post("", metered(accountResource::openAccount));
            get("", metered(accountResource::getAccount));
            get("/events", metered(accountResource::getEvents));
            put("/deposit", metered(accountResource::deposit));
            put("/withdraw", metered(accountResource::withdraw));
            put("/transfer", metered(accountResource::transfer));
            delete("", metered(accountResource::close));
        }));

        get("/ping", (req, res) -> "");

        exception(IllegalArgumentException.class, accountResource::badRequest);
        exception(AggregateNotFoundException.class, accountResource::notFound);
        exception(ConcurrentModificationException.class, accountResource::conflict);

        awaitInitialization();
        return port();
    }

    public void stop() {
        Spark.stop();
    }

    private Route metered(Route delegate) {
        return (request, response) -> {
            log.info("Request {} {}", request.requestMethod(), request.pathInfo());
            try {
                return delegate.handle(request, response);
            } finally {
                log.info("Responding with {}", response.status());
            }
        };
    }
}
