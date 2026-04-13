# ── Stage 1: Build ─────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Copy all module POMs so Maven can resolve the full reactor.
# This layer is cached as long as no pom.xml files change.
COPY pom.xml .
COPY outbox/pom.xml               outbox/pom.xml
COPY api-gateway/pom.xml          api-gateway/pom.xml
COPY auth-service/pom.xml         auth-service/pom.xml
COPY enrollment-service/pom.xml   enrollment-service/pom.xml
COPY payment-service/pom.xml      payment-service/pom.xml
COPY exam-service/pom.xml         exam-service/pom.xml
COPY grading-service/pom.xml      grading-service/pom.xml
COPY certificate-service/pom.xml  certificate-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml

# Pre-download all external dependencies (cached layer — skipped on source-only changes).
RUN mvn -B dependency:resolve -pl api-gateway -q

# Copy source code for this service only.
COPY api-gateway/src api-gateway/src

# Compile and package.
RUN mvn -B clean package -pl api-gateway -DskipTests

# ── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/api-gateway/target/api-gateway-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
