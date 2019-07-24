## Accounts

A simple, frameworkless event sourced Account implementation.

### Implementation


### Tests


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


### Building

```
./gradlew build
```

The build will only run the fast unit tests (including event store tests with H2).

In order to run the same set of tests targeting MySql, run
```
./gradlew integrationTest
```
Those will be much slower - they spawn the actual mysql instance using testcontainers and thus
require a running docker daemon.


### Running

Service can be started with h2 in memory database using
```
./gradlew run
```

or, packaged in a docker container and connected to a mysql container using:
```
./gradlew build
docker-compose up --build
```

### Dependencies

Kept external dependencies to a minimum, here's what's used and what for:
- Spark for spawning an embedded Jetty and glue for http request routing.
- Flyway - for DB migrations. Could have been done by hand, however Flyway is lightweight and brings
  no unwanted transitive dependencies.
- Failsafe for retrying mysql connections on startup. No brainer to implement by hand, however
  this library is lightweight and comes dependency-free.
- Lombok - for reducing boilerplate code and providing value objects. I'm not sure I like it.
- Logback - logging
- Jackson - for serializing events for storage. Not the best way of doing it. Would like to replace with protobuf.
- H2 - in memory database
- MySql connector - since the storage part is tested in mysql, might as well want to spawn the
  service connected to mysql.

Here is the actual runtime dependency tree, excluding Spark (which brings a bunch of Jetty ones):
```
+--- org.projectlombok:lombok:1.18.8
+--- com.fasterxml.jackson.core:jackson-databind:2.9.8
|    +--- com.fasterxml.jackson.core:jackson-annotations:2.9.0
|    \--- com.fasterxml.jackson.core:jackson-core:2.9.8
+--- org.flywaydb:flyway-core:5.2.4
+--- ch.qos.logback:logback-classic:1.2.3
|    +--- ch.qos.logback:logback-core:1.2.3
|    \--- org.slf4j:slf4j-api:1.7.25
+--- net.jodah:failsafe:2.1.0
+--- com.h2database:h2:1.4.199
\--- mysql:mysql-connector-java:8.0.16
     \--- com.google.protobuf:protobuf-java:3.6.1
```
Weird to see mysql connector bringing in protobuf transitive dependency, but oh well.