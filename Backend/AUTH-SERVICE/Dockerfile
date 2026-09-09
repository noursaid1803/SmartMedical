# ============================================================
# AUTH-SERVICE Dockerfile - Multi-stage build
# ============================================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copier le pom.xml et télécharger les dépendances (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copier le code source et builder
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============================================================
# Image finale légère
# ============================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Ajouter un utilisateur non-root pour la sécurité
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/target/*.jar app.jar

RUN chown spring:spring app.jar
USER spring

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=docker", "app.jar"]
