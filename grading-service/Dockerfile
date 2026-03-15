FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/grading-service-*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
