# Etapa 1: Construcción (Build)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución (Run)
# Usamos la imagen oficial de Playwright versión 1.40.0 (la misma que tu pom.xml)
# Esta imagen ya tiene preinstalados todos los navegadores y librerías gráficas necesarias.
FROM mcr.microsoft.com/playwright:v1.40.0-jammy
WORKDIR /app

# Como la imagen de Playwright es de Node/Ubuntu, le instalamos Java 21 para correr Spring Boot
USER root
RUN apt-get update && apt-get install -y openjdk-21-jre-headless && apt-get clean && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
