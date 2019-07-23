package lt.rieske.accounts.api;

import lt.rieske.accounts.eventsourcing.AggregateNotFoundException;
import spark.Spark;

public class Server {

    private final AccountResource accountResource;

    public Server(AccountResource accountResource) {
        this.accountResource = accountResource;
    }

    public int start(int port) {
        Spark.port(port);

        Spark.post("/account/:accountId", accountResource::createAccount);
        Spark.get("/account/:accountId", accountResource::getAccount);
        Spark.exception(IllegalArgumentException.class, accountResource::badRequest);
        Spark.exception(AggregateNotFoundException.class, accountResource::notFound);

        Spark.awaitInitialization();
        return Spark.port();
    }

    public void stop() {
        Spark.stop();
    }
}
