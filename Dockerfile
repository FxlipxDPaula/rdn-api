# syntax=docker/dockerfile:1

# ---------- build ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Copia apenas o necessario para resolver dependencias e reaproveitar cache de camadas.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw \
    && ./mvnw -B -ntp -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp -DskipTests package

# ---------- runtime ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Executa sem privilegios: nao roda como root dentro do container.
RUN groupadd --system app \
    && useradd --system --gid app --create-home app

COPY --from=build /workspace/target/*.jar app.jar
RUN chown -R app:app /app

USER app
EXPOSE 8080

# PORT vem do ambiente (PaaS). Sem PORT, a aplicacao sobe em 8080 (application.properties).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
