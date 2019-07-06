FROM openjdk:11.0.3-jdk-stretch

EXPOSE 8080

ENTRYPOINT exec java \
       --illegal-access=deny \
  	   -jar /opt/service/accounts.jar

ADD build/libs/accounts.jar /opt/service/accounts.jar
