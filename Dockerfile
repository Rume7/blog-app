# Use the official Maven image to build the application
FROM maven:3.8.6-eclipse-temurin-17 AS build

# Set the working directory
WORKDIR /app

# Copy the pom.xml and the source code into the container
COPY pom.xml .
COPY src ./src

# Package the application (this runs `mvn clean package` to build the jar)
ARG VERSION
RUN mvn clean package -DskipTests

# The second stage: use a smaller JRE image for the runtime environment
FROM openjdk:17-jdk-slim

# Set the working directory for the runtime
WORKDIR /app

# Define the version argument
ARG VERSION

# Copy the jar file from the build image to the runtime image
# Use the version in the filename dynamically
#COPY --from=build /app/target/blog-app-${VERSION}.jar /app/blog-app.jar
COPY --from=build /app/target/*.jar /app/blog-app.jar

# Expose the application port (default for Spring Boot is 8080)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "/app/blog-app.jar"]