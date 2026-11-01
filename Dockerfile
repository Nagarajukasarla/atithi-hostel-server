# Stage 1: Build application JAR
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy dependencies and source code
COPY pom.xml .
COPY src ./src

# Package JAR skipping tests (handled in CI)
RUN mvn -B -DskipTests package

# Stage 2: Minimal runtime image
FROM eclipse-temurin:17-jre
WORKDIR /app

# Run as dedicated non-root user for security
RUN groupadd --system app \
    && useradd --system --gid app --uid 10001 app

# Copy built JAR and assign ownership
COPY --from=build /workspace/target/aavas-*.jar /app/app.jar
RUN chown app:app /app/app.jar

USER app
EXPOSE 8080

# Allocate 75% of container memory to JVM heap
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
