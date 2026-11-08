# syntax=docker/dockerfile:1
# Formatico's server image: the jar that runs by itself, which Gradle makes on this machine (`./gradlew :server:bootJar`),
# given to the build as the context `jar` (compose.local.yaml), on Java 21's runtime.
FROM eclipse-temurin:21-jre-alpine
ARG REVISION=unknown
LABEL org.opencontainers.image.title="Formatico server" \
      org.opencontainers.image.description="Formatico's server: encrypted shares, and web addresses fetched for a browser" \
      org.opencontainers.image.revision="$REVISION"
WORKDIR /app
COPY --from=jar server.jar /app/server.jar
# The shares are kept under /data, a volume: made here, so that a new volume takes its owner, a user with no rights.
RUN mkdir -p /data/shares && chown -R 65534:65534 /data
USER 65534:65534
ENV FORMATICO_SHARES_DIRECTORY=/data/shares
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/api/health || exit 1
# The heap takes three quarters of what the container is given, and a server out of memory stops, to be started again.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/server.jar"]
