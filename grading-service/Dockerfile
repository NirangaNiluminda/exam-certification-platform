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

# Install the ROOT POM into .m2 first (-N = non-recursive, no submodule compilation).
# Required because outbox's installed .pom declares this root as its <parent>;
# Maven follows that parent chain when reading outbox's artifact descriptor from .m2.
RUN mvn -B install -N -q

# Now install outbox into .m2 so downstream dependency resolution can find it.
COPY outbox/src outbox/src
RUN mvn -B clean install -pl outbox -DskipTests -q

# Pre-download all external dependencies for this service (cached layer).
# Skipped on source-only changes to this service.
RUN mvn -B dependency:resolve -pl grading-service -q

# Copy source code for this service and compile.
COPY grading-service/src grading-service/src
RUN mvn -B clean package -pl grading-service -DskipTests

# ── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/grading-service/target/grading-service-*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
