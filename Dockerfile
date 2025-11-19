# ======== STAGE 1: BUILD ========
FROM maven:3.9.7-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copia arquivos do projeto
COPY pom.xml .
COPY src ./src

# Build do uber-jar (contém todas as dependências)
RUN mvn clean package -Dquarkus.package.type=uber-jar -DskipTests

# ======== STAGE 2: RUNTIME (AWS Lambda) ========
FROM public.ecr.aws/lambda/java:21

WORKDIR /app

# Copia o uber-jar gerado
COPY --from=build /workspace/target/*-runner.jar /app/app.jar

# Copia templates ou recursos usados pela função
COPY --from=build /workspace/src/main/resources/templates/feedback-alert.html /app/templates/feedback-alert.html

# Configuração Lambda: define handler direto da função
# AWS Lambda invoca seu handler Java automaticamente (NotifyAdminFunction)
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
CMD []
