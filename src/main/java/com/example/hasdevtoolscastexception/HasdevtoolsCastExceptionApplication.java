package com.example.hasdevtoolscastexception;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class HasdevtoolsCastExceptionApplication {

	public static void main(String[] args) {
		SpringApplication.run(HasdevtoolsCastExceptionApplication.class, args);
	}
	
	@Bean
	CommandLineRunner commandLineRunner() {
	    return args -> {
	        
	    };
	}
}
