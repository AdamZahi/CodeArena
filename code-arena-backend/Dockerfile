# ============================================================
# CodeArena Backend — Multi-stage lightweight build
# Final image: ~200MB (eclipse-temurin:17-jre-alpine)
# ============================================================

# --- Stage 1: Build with Maven ---
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy Maven wrapper and POM first (cache dependencies)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source and build
COPY src src
RUN ./mvnw clean package -DskipTests -B \
    && mv target/*.jar target/app.jar

# --- Stage 2: Extract Spring Boot layers (for better caching) ---
FROM eclipse-temurin:17-jre-alpine AS layers
WORKDIR /app
COPY --from=build /app/target/app.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# --- Stage 3: Final minimal runtime ---
FROM eclipse-temurin:17-jre-alpine
LABEL maintainer="CodeArena Team"

# Security: run as non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app

# Copy Spring Boot layers (best → least cacheable)
COPY --from=layers /app/dependencies/ ./
COPY --from=layers /app/spring-boot-loader/ ./
COPY --from=layers /app/snapshot-dependencies/ ./
COPY --from=layers /app/application/ ./

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.launch.JarLauncher"]
