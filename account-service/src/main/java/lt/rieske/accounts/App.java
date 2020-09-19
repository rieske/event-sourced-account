package lt.rieske.accounts;

import io.micrometer.core.instrument.MeterRegistry;
import lt.rieske.accounts.api.ApiConfiguration;
import lt.rieske.accounts.eventstore.BlobEventStore;
import lt.rieske.accounts.eventstore.Configuration;
import lt.rieske.accounts.infrastructure.TracingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Objects;


public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        var port = ApiConfiguration.server(App::environmentVariableEventStoreProvider, System.getenv("ZIPKIN_URL")).start(8080);
        log.info("Server started on port: {}", port);
    }

    static BlobEventStore environmentVariableEventStoreProvider(TracingConfiguration tracingConfiguration, MeterRegistry meterRegistry) {
        try {
            return Configuration.blobEventStore(
                    getRequiredEnvVariable("JDBC_URL"), getRequiredEnvVariable("DB_USER"),
                    getRequiredEnvVariable("DB_PASSWORD"), tracingConfiguration, meterRegistry);
        } catch (SQLException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getRequiredEnvVariable(String variableName) {
        return Objects.requireNonNull(System.getenv(variableName), String.format("Environment variable '%s' is required", variableName));
    }

}
