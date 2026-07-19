package lt.rieske.accounts.api;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;
import lt.rieske.accounts.eventsourcing.AggregateNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.net.InetSocketAddress;
import java.util.ConcurrentModificationException;
import java.util.function.Consumer;


public class Server {

    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private final AccountResource accountResource;
    private Undertow undertow;

    Server(AccountResource accountResource) {
        this.accountResource = accountResource;
    }

    public int start(int port) {
        RoutingHandler routes = Handlers.routing()
                .get("/ping", exchange -> exchange.setStatusCode(200))
                .post("/api/account/{accountId}", handle(accountResource::openAccount))
                .get("/api/account/{accountId}", handle(accountResource::getAccount))
                .get("/api/account/{accountId}/events", handle(accountResource::getEvents))
                .put("/api/account/{accountId}/deposit", handle(accountResource::deposit))
                .put("/api/account/{accountId}/withdraw", handle(accountResource::withdraw))
                .put("/api/account/{accountId}/transfer", handle(accountResource::transfer))
                .delete("/api/account/{accountId}", handle(accountResource::close))
                .setFallbackHandler(exchange -> exchange.setStatusCode(404));

        undertow = Undertow.builder()
                .addHttpListener(port, "0.0.0.0")
                .setHandler(new BlockingHandler(routes))
                .build();
        undertow.start();

        int actualPort = undertow.getListenerInfo().getFirst().getAddress() instanceof InetSocketAddress addr
                ? addr.getPort()
                : port;
        log.info("Server started on port: {}", actualPort);
        return actualPort;
    }

    public void stop() {
        if (undertow != null) {
            undertow.stop();
            undertow = null;
        }
    }

    private HttpHandler handle(Consumer<HttpServerExchange> action) {
        return exchange -> {
            try {
                log.info("Request {} {}", exchange.getRequestMethod(), exchange.getRequestPath());
                action.accept(exchange);
            } catch (IllegalArgumentException e) {
                accountResource.badRequest(e, exchange);
            } catch (AggregateNotFoundException e) {
                accountResource.notFound(e, exchange);
            } catch (ConcurrentModificationException e) {
                accountResource.conflict(e, exchange);
            } finally {
                log.info("Responding with {}", exchange.getStatusCode());
                MDC.clear();
            }
        };
    }
}
