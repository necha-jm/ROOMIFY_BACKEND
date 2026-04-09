# Use Java 17 as base
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy project files
COPY src/main/java/com/ROOMIFY/Roomify .

# Build the project using Maven Wrapper
RUN ./mvnw clean package -DskipTests

# Expose the port your Spring Boot app will run on
EXPOSE 8080

# Run the JAR file
CMD ["java", "-jar", "target/roomify-0.0.1-SNAPSHOT.jar"]