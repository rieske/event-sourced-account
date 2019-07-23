package lt.rieske.accounts.api;

import lt.rieske.accounts.eventsourcing.AggregateNotFoundException;
import spark.Spark;

public class Server {


    private final AccountResource accountResource;

    Server(AccountResource accountResource) {
        this.accountResource = accountResource;
    }

    public int start(int port) {
        Spark.port(port);

        Spark.path("/account/:accountId", () -> {
            Spark.post("", accountResource::createAccount);
            Spark.get("", accountResource::getAccount);
            Spark.put("/deposit", accountResource::deposit);
            Spark.put("/withdraw", accountResource::withdraw);
            Spark.put("/transfer", accountResource::transfer);
            Spark.delete("", accountResource::close);
        });

        Spark.exception(IllegalArgumentException.class, accountResource::badRequest);
        Spark.exception(AggregateNotFoundException.class, accountResource::notFound);

        Spark.awaitInitialization();
        return Spark.port();
    }

    public void stop() {
        Spark.stop();
    }
}
