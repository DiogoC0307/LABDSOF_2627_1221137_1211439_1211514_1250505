# Build context is this folder:  docker build -t ghcr.io/<user>/<service>:latest apps/<service>
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
RUN chmod +x mvnw && ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
ARG OTEL_AGENT_VERSION=2.26.1
ADD --chmod=644 https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar
COPY --from=build /src/target/*-SNAPSHOT.jar app.jar
USER 1000
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","/app/app.jar"]
