FROM eclipse-temurin:20.0.1_9-jre
EXPOSE 8080

ENV SERVICE_NAME=account-service
ENV JAVA_OPTS -Xmx64m -Xms64m

ENTRYPOINT /opt/service/$SERVICE_NAME/bin/$SERVICE_NAME
HEALTHCHECK --interval=5s CMD curl -s -f http://localhost:8080/ping || exit 1

ADD $SERVICE_NAME.tar /opt/service/
