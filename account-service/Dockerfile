FROM eclipse-temurin:18-jdk

EXPOSE 8080

ENV JAVA_OPTS -Xmx64m -Xms64m

ENTRYPOINT /opt/service/account-service/bin/account-service
HEALTHCHECK --interval=5s CMD curl -s -f http://localhost:8080/ping || exit 1

ADD build/distributions/account-service.tar /opt/service/
