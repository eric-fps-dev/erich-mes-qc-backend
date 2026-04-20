# Use OpenJDK to run the Spring Boot application
FROM azul/zulu-openjdk:21

# Set the working directory
WORKDIR /app

# Expose port 8085
# EXPOSE 8091
EXPOSE 8090

# Accept an argument to set the active profile
ARG SPRING_PROFILES_ACTIVE
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}
ENV JAVA_OPTS="-Djava.awt.headless=true"

# Add the JAR file to the container
COPY target/mes-qc-service.jar mes-qc-service.jar

# Command to run the JAR service when the container starts
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/mes-qc-service.jar"]

# Optional: Add a health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8091/actuator/health || exit 1
    # CMD curl -f http://localhost:8090/actuator/health || exit 1