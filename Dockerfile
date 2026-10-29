
# Stage 1: Build the Spring Boot application
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package

# Stage 2: Run the application
FROM eclipse-temurin:17-jre

WORKDIR /app

RUN groupadd --system app \
    && useradd --system --gid app --uid 10001 app

COPY --from=build /workspace/target/aavas-*.jar /app/app.jar

RUN chown app:app /app/app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
