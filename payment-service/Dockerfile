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
# -am = also-make: Maven resolves deps for outbox (local library) + this service.
RUN mvn -B dependency:resolve -pl payment-service -am -q

# Copy source code for the outbox shared library and this service.
COPY outbox/src          outbox/src
COPY payment-service/src payment-service/src

# Compile and package. -am ensures outbox is compiled first.
RUN mvn -B clean package -pl payment-service -am -DskipTests

# ── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/payment-service/target/payment-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
