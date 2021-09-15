package com.example.uppercasersocketdemo;

import java.time.Duration;
import java.util.function.Function;

import reactor.core.publisher.Flux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class UppercaseGrpcDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(UppercaseGrpcDemoApplication.class, args);
	}

	@Bean
	public Function<String, String> uppercase() {
		return String::toUpperCase;
	}

	@Bean
	public Function<Flux<String>, Flux<String>> aggregate() {
		return inbound -> inbound.log().window(Duration.ofSeconds(10))
				.flatMap(w -> w.reduce("", (s1, s2) -> s1 + s2)).log();
	}

}
