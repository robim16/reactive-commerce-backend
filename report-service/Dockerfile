# ── Stage 1: build ─────────────────────────────────────────────────────────
# maven:3.9-eclipse-temurin-21 incluye tanto Maven como el JDK 21.
# eclipse-temurin:21-jdk-alpine NO tiene Maven — es solo el JDK.
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /workspace

# 1. Copiar el pom.xml raíz primero (parent BOM con versiones)
COPY pom.xml ./pom.xml

# 2. Copiar el pom.xml del módulo antes que el código fuente.
#    Esto permite que Docker cachee la capa de descarga de dependencias:
#    si solo cambia src/ pero no pom.xml, esta capa se reutiliza.
COPY report-service/pom.xml ./report-service/pom.xml

# 3. Descargar dependencias del módulo en una capa cacheable
RUN mvn -f report-service/pom.xml dependency:go-offline     -B --no-transfer-progress     -Dmaven.repo.local=/root/.m2/repository

# 4. Copiar el código fuente y compilar
COPY report-service/src ./report-service/src
RUN mvn -f report-service/pom.xml package     -DskipTests -B --no-transfer-progress     -Dmaven.repo.local=/root/.m2/repository

# ── Stage 2: runtime ────────────────────────────────────────────────────────
# Solo JRE Alpine: imagen mínima sin Maven ni fuentes (~200 MB vs ~600 MB)
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /workspace/report-service/target/*.jar app.jar

EXPOSE 8088

ENTRYPOINT ["java",   "-Djava.security.egd=file:/dev/./urandom",   "-Dreactor.netty.http.server.accessLogEnabled=true",   "-XX:+UseContainerSupport",   "-XX:MaxRAMPercentage=75.0",   "-jar", "app.jar"]
