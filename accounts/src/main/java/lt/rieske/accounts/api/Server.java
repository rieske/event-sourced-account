package lt.rieske.accounts.api;

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

    Server(AccountResource accountResource) {
        this.accountResource = accountResource;
    }

    public int start(int port) {
        port(port);

        path("/api", () -> {
            before("/*", this::logRequest);
            afterAfter("/*", this::logResponse);
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


        exception(IllegalArgumentException.class, accountResource::badRequest);
        exception(AggregateNotFoundException.class, accountResource::notFound);
        exception(ConcurrentModificationException.class, accountResource::conflict);

        awaitInitialization();
        return port();
    }

    public void stop() {
        Spark.stop();
    }

    private void logRequest(Request request, Response response) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        String queryString = request.queryString() == null ? "" : "?" + request.queryString();
        log.info("{} {}{}", request.requestMethod(), request.pathInfo(), queryString);
    }

    private void logResponse(Request request, Response response) {
        log.info("{} {}", response.status(),
                response.body() == null ? "" : response.body());
    }

}
