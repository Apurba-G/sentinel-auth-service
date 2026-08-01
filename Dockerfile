# ==============================================================================
# Stage 1: Build stage with Maven & Java 17 Alpine
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Copy pom.xml and download dependencies (Layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build application JAR
COPY src ./src
RUN mvn package -DskipTests

# ==============================================================================
# Stage 2: Lightweight Runtime stage
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a non-root system user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy built JAR from builder stage
COPY --from=builder /app/target/ecommerce-auth-service-1.0.0-SNAPSHOT.jar app.jar

# Expose microservice port
EXPOSE 8081

# Set active environment variables & JVM options
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Healthcheck endpoint using wget
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8081/v3/api-docs || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
