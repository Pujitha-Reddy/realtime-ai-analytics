# Stage 1: build the jar with Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: run it on a lightweight JRE-only image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/analytics-0.0.1-SNAPSHOT.jar app.jar

# Render injects PORT at runtime; our app.yml already reads ${PORT:8080}
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
