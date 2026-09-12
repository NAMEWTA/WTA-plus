FROM bellsoft/liberica-openjdk-rocky:21.0.12-cds

LABEL org.opencontainers.image.title="namewta-snailjob-server" \
      org.opencontainers.image.vendor="NAMEWTA"

RUN mkdir -p /wta/snailjob/logs
WORKDIR /wta/snailjob

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="" \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8

COPY app.jar ./app.jar

EXPOSE 8800 17888

ENTRYPOINT ["/bin/bash", "-c", "exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
