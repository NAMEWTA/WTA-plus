FROM bellsoft/liberica-openjdk-rocky:21.0.12-cds

LABEL org.opencontainers.image.title="namewta-snailai-server" \
      org.opencontainers.image.vendor="NAMEWTA"

RUN mkdir -p /wta/snailai/logs
WORKDIR /wta/snailai

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="" \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8

COPY app.jar ./app.jar

EXPOSE 8900 18888

ENTRYPOINT ["/bin/bash", "-c", "exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
