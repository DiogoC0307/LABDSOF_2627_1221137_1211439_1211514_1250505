# Build context is this folder:  docker build -t ghcr.io/<user>/<service>:latest apps/<service>
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY ../../../../../AppData/Local/Temp .
RUN chmod +x mvnw && ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/*-SNAPSHOT.jar app.jar
USER 1000
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","/app/app.jar"]
