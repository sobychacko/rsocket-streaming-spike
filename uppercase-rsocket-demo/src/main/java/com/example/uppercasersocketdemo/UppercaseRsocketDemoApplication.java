package com.example.uppercasersocketdemo;

import java.util.function.Function;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class UppercaseRsocketDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(UppercaseRsocketDemoApplication.class, args);
	}

	@Bean
	public Function<String, String> uppercase() {
		return String::toUpperCase;
	}

}
