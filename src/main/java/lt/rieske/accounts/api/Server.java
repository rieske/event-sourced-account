package lt.rieske.accounts.api;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import lt.rieske.accounts.eventsourcing.AggregateNotFoundException;
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


@Slf4j
public class Server {

    private final AccountResource accountResource;
    private final PrometheusMeterRegistry meterRegistry;

    Server(AccountResource accountResource, PrometheusMeterRegistry meterRegistry) {
        this.accountResource = accountResource;
        this.meterRegistry = meterRegistry;
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
