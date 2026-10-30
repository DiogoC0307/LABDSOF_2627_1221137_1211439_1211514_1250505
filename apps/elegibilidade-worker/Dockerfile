# Build context is this folder:  docker build -t ghcr.io/<user>/<service>:latest apps/<service>
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
RUN chmod +x mvnw && ./mvnw -B -ntp -DskipTests package

# OpenTelemetry Java agent (distributed tracing). It is downloaded and VERIFIED here, so a bad download
# fails the build instead of producing an image that cannot start. It is only active when
# JAVA_TOOL_OPTIONS points at it (set in the Kubernetes ConfigMaps and in compose.yaml).
FROM eclipse-temurin:21-jdk AS otel
ARG OTEL_AGENT_VERSION=2.26.1
RUN mkdir -p /otel \
 && ( curl -fsSL -o /otel/opentelemetry-javaagent.jar "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar" \
      || wget -q -O /otel/opentelemetry-javaagent.jar "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar" ) \
 && jar tf /otel/opentelemetry-javaagent.jar | grep -q '^META-INF/MANIFEST.MF$' \
 && chmod 755 /otel && chmod 644 /otel/opentelemetry-javaagent.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=otel /otel /otel
RUN chmod -R a+rX /otel
COPY --from=build /src/target/*-SNAPSHOT.jar app.jar
USER 1000
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","/app/app.jar"]
