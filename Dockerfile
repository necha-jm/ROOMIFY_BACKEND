# Use Debian-based Java 17 image
FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

# Make mvnw executable inside Docker
RUN chmod +x mvnw

# Build project
RUN ./mvnw clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "target/roomify-0.0.1-SNAPSHOT.jar"]