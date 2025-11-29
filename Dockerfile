# Dockerfile (Java Backend)

# Stage 1: Building
FROM gradle:8.5-jdk17 AS builder
# ... (rest of stage 1 remains unchanged) ...

# -----------------------------------------------------------
# Stage 2: Running (Fix applied here)
# -----------------------------------------------------------
# Use the official OpenJDK JRE image with the slim tag (or alpine for smallest size)
FROM openjdk:17-jdk-slim
# OR, the most common working tag:
# FROM openjdk:17-jdk-slim

# Let's use 'eclipse-temurin:17-jdk' as it's the modern, supported replacement for the deprecated openjdk image
FROM eclipse-temurin:17-jdk as runtime
COPY --from=builder /home/gradle/src/ubos-server/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]