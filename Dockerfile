FROM openjdk:14-jdk AS BUILD_IMAGE
ENV APP_HOME=/opt/build
RUN mkdir -p $APP_HOME
WORKDIR $APP_HOME
COPY gradlew $APP_HOME/
COPY gradle/ $APP_HOME/gradle/
COPY src/ $APP_HOME/src/
COPY build.gradle settings.gradle $APP_HOME/
RUN ./gradlew build runtime -x integrationTest -x e2eTest

FROM panga/alpine:3.8-glibc2.27
WORKDIR /root/
COPY --from=BUILD_IMAGE /opt/build/build/image/ /opt/service/
EXPOSE 8080
ENV JAVA_OPTS --illegal-access=deny -Xmx64m -Xms64m --enable-preview
ENTRYPOINT /opt/service/bin/account
