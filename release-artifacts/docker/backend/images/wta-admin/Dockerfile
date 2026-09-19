FROM bellsoft/liberica-openjdk-rocky:21.0.12-cds

LABEL org.opencontainers.image.title="namewta-admin" \
      org.opencontainers.image.vendor="NAMEWTA"

RUN mkdir -p /wta/server/logs /wta/server/temp
WORKDIR /wta/server

ENV SERVER_PORT=8080 \
    SNAIL_JOB_PORT=28080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="" \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8

COPY app.jar ./app.jar

EXPOSE 8080 28080

ENTRYPOINT ["/bin/bash", "-c", "exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -Dserver.port=${SERVER_PORT} -Dsnail-job.port=${SNAIL_JOB_PORT} -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
