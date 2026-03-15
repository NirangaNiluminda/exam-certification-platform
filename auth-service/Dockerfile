FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar
COPY target/*.jar app.jar

# JWT keys directory (mounted via docker-compose volume or K8s secret)
RUN mkdir -p /keys

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
