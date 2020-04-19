## Event Sourced Account

[![Actions Status](https://github.com/rieske/event-sourced-account/workflows/build/badge.svg)](https://github.com/rieske/event-sourced-account/actions)

A simple, frameworkless event sourced Account implementation.

### Implementation

The service is test driven bottom up: domain -> event sourcing -> external API.


#### The basics
I started with the basic operations in the account - deposit and withdrawal, open and close.
This captures the basic business rules of when an account can be interacted with, what amounts
can be deposited and withdrawn and under what circumstances.
Money transfer between accounts is nothing but a withdrawal from one account and deposit to
another that has to happen within a transaction.


#### Concurrency
Then follows the event sourcing part - I'm aware of consistency issues when concurrent
modifications are attempted and event sourced implementation can easily prevent those
as well as provide a transaction log, potential to build custom projections of past events
and potential to emit certain external events for other systems to react to.

Firstly, a naive in-memory event store backed by Maps and Lists - after basic logic
was in place and events were emitted from operations with account, I had to test for potential
concurrency issues that might arise as the service can be called by many external clients at
the same time. The not-so-nice account consistency test where a single account is hammered
by multiple threads was quick to reveal some of the things that will go wrong in a multithreaded
environment.

After patching the in-memory event store, I drove the implementation for a
sql event store, initially serializing the payloads to json and later to msgpack - both to
ensure extensibility and to be as lightweight as possible. Then, extended the same tests
that I used with in-memory event store to also run with H2 embedded database. And then with
MySql just to be sure (those are slow ones and are not part of the main build). At this point,
I was able to validate all the core functionality, including consistency very quickly with
multiple event store implementations. No mocks, all the tests exercise the full functionality via
the eventstore API.
And event store here is key to ensuring consistency in a multithreaded environment.
Specifically the constraints that the database provides - remove the (aggregateId, sequenceNumber)
primary key and all but consistency tests will pass.

#### External API
Next came the external API, repeated some of the same tests for basic functions with RestAssured,
plugged in H2 event store and drove the implementation with the help of Spark.


#### Idempotency
What can be important when dealing with money and especially when requests come over an
unreliable network (and network is unreliable by definition) is idempotency. When a client
request gets interrupted due to whatever reason, the client might not know whether the
request was handled or not and might retry. This might result in a double transfer, double
deposit or withdrawal had the original request been handled successfully. To prevent such
cases, the client should supply a unique transaction id (a UUID in our case) for each
distinct operation. This id is persisted and in case a duplicate request comes in, it will
be accepted, but no action taken since we know we already handled it.
Transaction ids can not be reused. Current implementation is a bit naive as it does not take into account
the type of operation in context of idempotency, just the transaction id together with
affected account id, meaning that given a transaction id that was used for a deposit
would be used for a withdrawal, the service would respond that it accepted the request.
Maybe a better way would be to conflict on such cases.


### API

- open account: `POST /api/account/{accountId}?owner={ownerId}` should respond with `201`
  and a `Location` header pointing to the created resource if successful
- get account's current state: `GET /api/account/{accountId}` should respond with `200`
  and a json body if account is found, otherwise `404`
- deposit: `PUT /api/account/{accountId}?deposit={amount}&transactionId={uuid}`
  should respond with `204` if successful
- withdraw: `PUT /api/account/{accountId}?withdraw={amount}&transactionId={uuid}`
  should respond with `204` if successful
- transfer: `PUT /api/account/{accountId}?transfer={targetAccountId}&amount={amount}&transactionId={uuid}`
  should respond with `204` if successful
- close account: `DELETE /api/account/{accountId}` should respond with `204` if successful


### Tests

Tests are separated in their own source sets and given their own Gradle task by test category.
Tests in the `test` source set and executed by `test` task are the fast unit tests.

Next level are the integration tests that use both Postgres and Mysql backed event store implementations.
They use testcontainers to spawn real database instances. Integration tests live in `integrationTest` source
set and are executed using `integrationTest` task.

Finally, a couple of end-to-end tests that focus mainly on sanity testing consistency in a distributed
environment. `e2eTest` source set and a task with the same name.

Since I was test driving this service from the domain up to the event sourcing infrastructure and lastly
up to the API, some of the tests might be redundant and functionality might be tested several times.
With in-memory event store and h2 event store, the unit tests exercise the whole module really fast
and it might even be possible to move the remaining lower level tests higher up. This would allow
to test the functionality solely via the API and not have any implementation details tests.
Which can in turn make changes to internal implementation details easier to make with absence of
implementation oriented tests. I did not use any mocking framework to avoid testing implementation
and focus solely on the functionality.


### Potential red flags

- I used longs for monetary amounts, assuming those are in minor units/cents. I am aware that money
is a delicate matter and extra care is needed when dealing with it in software. Since
this service performs only basic addition and subtraction, I decided to use cents for now
and focus on other things. Should I need to deal with floating points, I would at the very
least go for BigDecimal and probably do some investigation around current best practices -
I know there is a javamoney implementation, also an older joda money one.

- I did not take currency into account - all accounts are same currency for now. Should I
need to add currency, I'd probably have to refine the type that holds amounts to include
the currency, prevent deposits/withdrawals if account currency does not match. This would
prevent cross currency transfers right away. Currency conversion for cross currency transfers
is something I'd have to figure out. This potentially could be out of scope for accounts
service itself.

- Exception types in domain - probably would make sense to create specific types for
InsufficientBalance, AccountClosed exceptions etc. Kept it simple with IllegalArgument/State
exceptions for the sake of avoiding unneeded class count explosion.

### Building

```shell script
./gradlew test
```

The test task will only run the fast unit tests (including event store tests with H2 in postgres mode).

In order to run the same set of tests targeting Postgres and Mysql, run
```shell script
./gradlew integrationTest
```
Those will be much slower - they spawn the actual Postgres and Mysql instances using testcontainers and thus
require a running docker daemon.

And another round of slow tests that test for consistency in a distributed environment:
```shell script
./gradlew e2eTest
```
Those will spawn a docker-composed environment with two service instances connected to
a postgres container and a load balancer on top. Tests will be executed against the load balancer,
simulating a distributed environment and asserting that the service can scale and remain consistent.

To run the full suite, run:
```shell script
./gradlew check
```

### Running

The service can be spawned in a minimal production-like environment using `docker-compose`.
The environment consists of two service instances packaged in a docker container, connected to a Postgres container and
exposed via Envoy Proxy. A minimal monitoring setup is available as well.

To start:
```shell script
./gradlew build
docker-compose up --build
```
The service will be accessible on localhost:8080 and requests
will go via a load balancer to two service instances in a round robin fashion.

### Monitoring

Tracing instrumentation with Zipkin is provided if the service is started with ZIPKIN_URL environment
variable set. It is preconfigured in the composed environment.
Basic metrics are exposed to Prometheus and sample configuration of Prometheus together with 
Grafana and a service/envoy dashboards can be accessed by spawning a composed environment using
```shell script
./gradlew build
docker-compose up --build
```
Prometheus is exposed on port 9090.
Grafana is available on port 3000.
Zipkin on port 9411.

### Dependencies

Kept external dependencies to a minimum, here's what's used and what for:
- Spark for spawning an embedded Jetty and glue for http request routing.
- Flyway - for DB migrations. Could have been done by hand, however Flyway is lightweight and brings
  no unwanted transitive dependencies.
- Logback - logging
- msgpack - serializing events for storage.
- H2 - in memory database
- MySql connector - since the storage part is tested in mysql, might as well want to spawn the
  service connected to mysql.
- HikariCP - database connection pooling
- Micrometer/Prometheus - metrics
- Brave instrumentation for zipkin

Here is the runtime dependency tree:
```
+--- com.sparkjava:spark-core:2.9.1                                                                                                                               [14/9139]
|    +--- org.slf4j:slf4j-api:1.7.25
|    \--- org.eclipse.jetty:jetty-server:9.4.18.v20190429
|         +--- javax.servlet:javax.servlet-api:3.1.0
|         +--- org.eclipse.jetty:jetty-http:9.4.18.v20190429
|         |    +--- org.eclipse.jetty:jetty-util:9.4.18.v20190429
|         |    \--- org.eclipse.jetty:jetty-io:9.4.18.v20190429
|         |         \--- org.eclipse.jetty:jetty-util:9.4.18.v20190429
|         \--- org.eclipse.jetty:jetty-io:9.4.18.v20190429 (*)
+--- org.msgpack:msgpack-core:0.8.20
+--- org.flywaydb:flyway-core:6.3.3
+--- ch.qos.logback:logback-classic:1.2.3
|    +--- ch.qos.logback:logback-core:1.2.3
|    \--- org.slf4j:slf4j-api:1.7.25
+--- io.micrometer:micrometer-registry-prometheus:1.4.1
|    +--- io.micrometer:micrometer-core:1.4.1
|    |    +--- org.hdrhistogram:HdrHistogram:2.1.12
|    |    \--- org.latencyutils:LatencyUtils:2.0.3
|    \--- io.prometheus:simpleclient_common:0.8.1
|         \--- io.prometheus:simpleclient:0.8.1
+--- io.zipkin.brave:brave-instrumentation-sparkjava:5.11.2
|    +--- io.zipkin.brave:brave-instrumentation-servlet:5.11.2
|    |    +--- io.zipkin.brave:brave-instrumentation-http:5.11.2
|    |    |    \--- io.zipkin.brave:brave:5.11.2
|    |    |         +--- io.zipkin.zipkin2:zipkin:2.19.3
|    |    |         \--- io.zipkin.reporter2:zipkin-reporter:2.12.1
|    |    |              \--- io.zipkin.zipkin2:zipkin:2.19.3
|    |    \--- io.zipkin.brave:brave:5.11.2 (*)
|    \--- io.zipkin.brave:brave:5.11.2 (*)
+--- io.zipkin.brave:brave-instrumentation-p6spy:5.11.2
|    +--- p6spy:p6spy:3.8.7
|    \--- io.zipkin.brave:brave:5.11.2 (*)
+--- io.zipkin.reporter2:zipkin-sender-urlconnection:2.12.1
|    +--- io.zipkin.reporter2:zipkin-reporter:2.12.1 (*)
|    \--- io.zipkin.zipkin2:zipkin:2.19.3
+--- com.zaxxer:HikariCP:3.4.2
|    \--- org.slf4j:slf4j-api:1.7.25
+--- mysql:mysql-connector-java:8.0.19
\--- org.postgresql:postgresql:42.2.12
```
