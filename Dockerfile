# ==============================================================================
# Stage 1: Build stage with Maven & Java 17 Alpine
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Copy pom.xml and download dependencies (Layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and build application JAR
COPY src ./src
RUN mvn package -DskipTests

# ==============================================================================
# Stage 2: Lightweight Zero-Trust Runtime
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a non-root system user for container security
RUN addgroup -S sentinelgroup && adduser -S sentineluser -G sentinelgroup
USER sentineluser

# Copy built JAR from builder stage
COPY --from=builder /app/target/sentinel-auth-core-*.jar app.jar

# Expose microservice port
EXPOSE 8081

# Optimize JVM for cloud container memory constraints
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

# Healthcheck endpoint using wget
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8081/v3/api-docs || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
