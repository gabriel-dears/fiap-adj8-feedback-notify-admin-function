FROM eclipse-temurin:21-jre

WORKDIR /app

# Renomeia o JAR padrão
COPY target/*-runner.jar /app/function.jar

COPY src/main/resources/templates/feedback-alert.html /app/templates/feedback-alert.html

# Configurações para o servidor HTTP
EXPOSE 8080
ENV PORT=8080
ENV QUARKUS_HTTP_PORT=8080

# NOVO ENTRYPOINT: Usa o Funqy e aponta para o nome QUALIFICADO da classe.
# O Quarkus Funqy precisa desse hint para saber qual classe gerenciar.
ENTRYPOINT ["java", "-Dquarkus.funqy.export-path=/", "-jar", "/app/function.jar"]