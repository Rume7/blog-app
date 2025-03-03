# Use a specific version of the OpenJDK image
FROM openjdk:17-alpine
RUN apk add --no-cache curl

# Set the working directory
WORKDIR /app

# Copy the JAR file into the container
COPY target/blog-app-1.0-SNAPSHOT.jar app.jar

# Set the entry point for the container
ENTRYPOINT ["java", "-jar", "app.jar"]