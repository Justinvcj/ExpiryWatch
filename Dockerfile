FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
# Install Tesseract for Phase 2 OCR
RUN apt-get update && apt-get install -y tesseract-ocr && rm -rf /var/lib/apt/lists/*
COPY --from=builder /app/target/*.jar app.jar
# Limit JVM RAM to ~256m so Tesseract has space on the 512MB Render free tier
ENTRYPOINT ["java", "-Xmx256m", "-jar", "app.jar"]
