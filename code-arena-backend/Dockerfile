# syntax=docker/dockerfile:1.6

# ---------- Stage 1: build ----------
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Tools needed by the Maven Wrapper on alpine
RUN apk add --no-cache bash

# Maven flags tuned for slow / unreliable networks
ENV MAVEN_OPTS="-Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 -Dhttp.keepAlive=false"

# Copy the entire project (mvnw, pom.xml, sources)
COPY . .

# Make wrapper executable then build the JAR. We do NOT pre-resolve with
# dependency:go-offline — it doubles downloads and times out on slow links.
RUN chmod +x mvnw \
    && ./mvnw clean package -DskipTests -B \
        -Dmaven.wagon.http.retryHandler.count=5 \
        -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 \
        -Dhttp.keepAlive=false

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the fat JAR (Spring Boot Maven Plugin produces a single executable jar)
COPY --from=build /app/target/*.jar /app/app.jar
RUN chown -R spring:spring /app

USER spring:spring

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
