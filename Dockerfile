FROM panga/alpine:3.8-glibc2.27

EXPOSE 8080

ENV JAVA_OPTS --illegal-access=deny -Xmx64m -Xms64m --enable-preview

ENTRYPOINT /opt/service/bin/account

ADD build/image/ /opt/service/