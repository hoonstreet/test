FROM maven:3.9.11-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src src
RUN mvn -B package

FROM eclipse-temurin:21-jre-noble
RUN useradd --system --uid 10001 --create-home appuser
WORKDIR /app
COPY --from=builder /workspace/target/cms-*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=65.0", "-jar", "/app/app.jar"]
