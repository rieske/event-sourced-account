package lt.rieske.accounts;

import lt.rieske.accounts.api.ApiConfiguration;
import lt.rieske.accounts.eventstore.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;


public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        var port = ApiConfiguration.server(
                (tracing, metrics) -> Configuration.blobEventStore(
                        getRequiredEnvVariable("JDBC_URL"), getRequiredEnvVariable("DB_USER"),
                        getRequiredEnvVariable("DB_PASSWORD"), tracing, metrics),
                System.getenv("OPENTELEMETRY_URL"),
                System.getenv("ZIPKIN_URL")).start(8080);
        log.info("Server started on port: {}", port);
    }

    private static String getRequiredEnvVariable(String variableName) {
        return Objects.requireNonNull(System.getenv(variableName), String.format("Environment variable '%s' is required", variableName));
    }
}
