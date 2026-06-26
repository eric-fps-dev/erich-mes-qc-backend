# =============================================================================
# Stage 1: Builder
# Uses Maven with Eclipse Temurin JDK 21 to compile and package the application.
# Dependencies are resolved first (before copying source) to leverage Docker
# layer caching — rebuilds only re-run the build step if source code changes.
# =============================================================================
FROM maven:3.9-eclipse-temurin-21 AS builder

# Set the working directory for the build
WORKDIR /build

# Copy custom Maven settings to allow the private Nexus HTTP repository.
# (Maven 3.8.1+ blocks HTTP repos by default; this override permits it.)
COPY settings.xml .

# Copy only the POM first to resolve and cache dependencies as a separate layer.
COPY pom.xml .
RUN mvn dependency:resolve -B -s settings.xml

# Copy the application source code and build the JAR (skip tests for speed)
COPY src ./src
RUN mvn clean package -DskipTests -B -s settings.xml


# =============================================================================
# Stage 2: Runtime
# Uses a minimal Azul Zulu JRE 21 image to run the application.
# Only the built JAR is copied from the builder stage, keeping the final
# image small and free of build tools.
# =============================================================================
FROM azul/zulu-openjdk:21-jre

# Set the working directory for the application
WORKDIR /app

# Install font libraries
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Expose the application port
EXPOSE 8090

# Accept an argument to set the active Spring profile at build time
ARG SPRING_PROFILES_ACTIVE
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}

# JVM options — run in headless mode (no GUI dependencies)
ENV JAVA_OPTS="-Djava.awt.headless=true"

# Copy the built JAR from the builder stage
COPY --from=builder /build/target/mes-qc-service.jar mes-qc-service.jar

# Command to run the JAR service when the container starts
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -jar /app/mes-qc-service.jar"]

# Optional: Add a health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8090/api/qc/actuator/health || exit 1