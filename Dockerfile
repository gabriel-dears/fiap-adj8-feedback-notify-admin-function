# ======== STAGE 1: BUILD ========
FROM maven:3.9.7-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn clean compile -DskipTests

RUN mvn package -DskipTests

# ======== STAGE 2: RUNTIME ========
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/target/*-runner.jar /app/function.jar

COPY --from=build /workspace/src/main/resources/templates/feedback-alert.html /app/templates/feedback-alert.html

EXPOSE 8080
ENV PORT=8080
ENV QUARKUS_HTTP_PORT=8080

ENTRYPOINT ["java", "-Dquarkus.funqy.export-path=/", "-jar", "/app/function.jar"]
