package lt.rieske.accounts.api;

import io.helidon.integrations.micrometer.MeterRegistryFactory;
import io.helidon.integrations.micrometer.MicrometerSupport;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lt.rieske.accounts.eventsourcing.AggregateNotFoundException;
import lt.rieske.accounts.infrastructure.TracingConfiguration;

import java.util.ConcurrentModificationException;
import java.util.Optional;
import java.util.function.Supplier;


public class Server {

    private final PrometheusMeterRegistry meterRegistry;
    private final TracingConfiguration tracingConfiguration;
    private final WebServer webServer;

    Server(AccountResource accountResource, PrometheusMeterRegistry meterRegistry, TracingConfiguration tracingConfiguration, int port) {
        this.meterRegistry = meterRegistry;
        this.tracingConfiguration = tracingConfiguration;

        MicrometerSupport micrometerSupport = MicrometerSupport.builder()
                .meterRegistryFactorySupplier(MeterRegistryFactory.builder()
                        .enrollRegistry(meterRegistry, serverRequest -> Optional.empty())
                        .build()
                ).build();

        Supplier<Routing> routing = () -> Routing.builder()
                .register(micrometerSupport)

                .post("/api/account/{accountId}", metered(accountResource::openAccount, "open_account"))
                .get("/api/account/{accountId}", metered(accountResource::getAccount, "query_account"))
                .get("/api/account/{accountId}/events", metered(accountResource::getEvents, "query_account_events"))
                .put("/api/account/{accountId}/deposit", metered(accountResource::deposit, "deposit"))
                .put("/api/account/{accountId}/withdraw", metered(accountResource::withdraw, "withdraw"))
                .put("/api/account/{accountId}/transfer", metered(accountResource::transfer, "transfer"))
                .delete("/api/account/{accountId}", metered(accountResource::close, "close_account"))


                .get("/ping", (req, res) -> res.send())
                .get("/prometheus", (req, res) -> res.send(meterRegistry.scrape()))

                .error(IllegalArgumentException.class, accountResource::badRequest)
                .error(AggregateNotFoundException.class, accountResource::notFound)
                .error(ConcurrentModificationException.class, accountResource::conflict)

                .build();

        this.webServer = WebServer.builder()
                .bindAddress("0.0.0.0")
                .port(port)
                .routing(routing)
                .tracer(tracingConfiguration.tracer())
                .build();
    }

    public int start() {
        return webServer.start()
                .await()
                .port();
    }

    public void stop() {
        webServer.shutdown().await();
        tracingConfiguration.closeResources();
    }

    private Handler metered(Handler delegate, String metricTag) {
        var timer = Timer.builder("request_latency").tags("operation", metricTag)
                .publishPercentileHistogram()
                .register(meterRegistry);
        // TODO: this probably won't cut it if async stuff is happening behind the scenes
        return (request, response) -> timer.record(() -> delegate.accept(request, response));
    }
}
