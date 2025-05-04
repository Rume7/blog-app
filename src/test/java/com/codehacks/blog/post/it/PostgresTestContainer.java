package com.codehacks.blog.post.it;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class PostgresTestContainer extends PostgreSQLContainer<PostgresTestContainer> {

    private static final String IMAGE_VERSION = "postgres:16-alpine";
    private static PostgresTestContainer container;

    private PostgresTestContainer() {
        super(DockerImageName.parse(IMAGE_VERSION));
        withDatabaseName("blog_db_test");
        withUsername("test");
        withPassword("test");
        withReuse(true); // Enable reuse of the container
    }

    public static PostgresTestContainer getInstance() {
        if (container == null) {
            container = new PostgresTestContainer();
            container.start();  // Starts the container only once
        }
        return container;
    }

    @Override
    public void start() {
        super.start();
        // Set system properties for Spring
        System.setProperty("spring.datasource.url", getJdbcUrl());
        System.setProperty("spring.datasource.username", getUsername());
        System.setProperty("spring.datasource.password", getPassword());
    }

    @Override
    public void stop() {
        // We do nothing here as the container is reused
    }
}

