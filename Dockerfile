# ===== BUILD =====
FROM maven:3.9.7-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# ===== RUNTIME =====
FROM eclipse-temurin:21-jre
WORKDIR /function

# Copia o JAR gerado
COPY --from=build /workspace/target/function-runner.jar ./
COPY --from=build /workspace/target/lib ./lib

# Opcional: bootstrap script para AWS Lambda
COPY --from=build /workspace/target/bootstrap-example.sh ./bootstrap
RUN chmod +x bootstrap

ENTRYPOINT ["./bootstrap"]
