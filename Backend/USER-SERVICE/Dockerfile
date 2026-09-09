# ============================================================
# USER-SERVICE Dockerfile - Multi-stage build
# ============================================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ============================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/target/*.jar app.jar

RUN chown spring:spring app.jar
USER spring

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=docker", "app.jar"]
