FROM openjdk:11.0.5-jre-stretch

EXPOSE 8080

ENV JAVA_OPTS --illegal-access=deny -Xmx64m -Xms64m

ENTRYPOINT /opt/service/account/bin/account

ADD build/distributions/account.tar /opt/service/
