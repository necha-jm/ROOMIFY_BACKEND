# Use Java 21
FROM eclipse-temurin:21-jdk

# Set working directory
WORKDIR /app

# Copy project files
COPY . .

# Make mvnw executable inside Docker
RUN chmod +x mvnw

# Build the project
RUN ./mvnw clean package -DskipTests

# Expose port
EXPOSE 8080

# Run JAR file
CMD ["java", "-jar", "target/roomify-0.0.1-SNAPSHOT.jar"]