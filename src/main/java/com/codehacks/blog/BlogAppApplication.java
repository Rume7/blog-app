package com.codehacks.blog;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.codehacks.blog")
public class BlogAppApplication {

	public static void main(String[] args) {
		Dotenv.configure().load(); // Load the .env file
		SpringApplication.run(BlogAppApplication.class, args);
	}
}
