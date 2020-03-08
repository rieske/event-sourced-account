FROM openjdk:14-jdk-slim

EXPOSE 8080

ENV JAVA_OPTS --illegal-access=deny -Xmx64m -Xms64m

ENTRYPOINT /opt/service/account/bin/account

ADD build/distributions/account.tar /opt/service/
