FROM openjdk:17-jdk-slim
COPY target/blog-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
