FROM eclipse-temurin:19.0.1_10-jre
EXPOSE 8080

ENV SERVICE_NAME=account-service
ENV JAVA_OPTS -Xmx64m -Xms64m

ENTRYPOINT /opt/service/$SERVICE_NAME/bin/$SERVICE_NAME
HEALTHCHECK --interval=5s CMD curl -s -f http://localhost:8080/ping || exit 1

ADD $SERVICE_NAME.tar /opt/service/
