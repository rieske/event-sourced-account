FROM openjdk:14-jdk-slim

EXPOSE 8080

ENV JAVA_OPTS --illegal-access=deny -Xmx64m -Xms64m --enable-preview

ENTRYPOINT /opt/service/account-service/bin/account-service

ADD build/distributions/account-service.tar /opt/service/
