FROM openjdk:11.0.3-jdk-stretch

EXPOSE 8080

ENV JAVA_OPTS --illegal-access=deny -Xmx64m -Xms64m

ENTRYPOINT /opt/service/accounts/bin/accounts

ADD build/distributions/accounts.tar /opt/service/
