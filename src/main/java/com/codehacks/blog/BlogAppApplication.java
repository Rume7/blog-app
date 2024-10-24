package com.codehacks.blog;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BlogAppApplication {

	public static void main(String[] args) {
		Dotenv.load(); // Load the .env file
		SpringApplication.run(BlogAppApplication.class, args);
	}

}
