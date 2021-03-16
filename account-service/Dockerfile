FROM openjdk:16-jdk-slim

EXPOSE 8080

ENV JAVA_OPTS -Xmx64m -Xms64m

ENTRYPOINT /opt/service/account-service/bin/account-service

ADD build/distributions/account-service.tar /opt/service/
