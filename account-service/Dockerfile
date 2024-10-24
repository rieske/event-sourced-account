FROM eclipse-temurin:21.0.5_11-jre
EXPOSE 8080

ENV SERVICE_NAME=account-service
ENV JAVA_OPTS -XX:+ExitOnOutOfMemoryError -Xmx64m -Xms64m -XX:MaxMetaspaceSize=128m

ENV OTEL_SERVICE_NAME=account-service
ENV OTEL_EXPORTER_OTLP_PROTOCOL=grpc
ENV OTEL_INSTRUMENTATION_LOGBACK_APPENDER_EXPERIMENTAL_CAPTURE_MDC_ATTRIBUTES=*
ENV OTEL_EXPERIMENTAL_RESOURCE_DISABLED_KEYS=process.command_args,process.runtime.name,process.runtime.description,process.executable.path,process.pid

ENTRYPOINT /opt/service/$SERVICE_NAME/bin/$SERVICE_NAME
HEALTHCHECK --interval=5s CMD curl -s -f http://localhost:8080/ping || exit 1

ADD $SERVICE_NAME.tar /opt/service/
