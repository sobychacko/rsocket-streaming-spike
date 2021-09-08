package com.example.multibinderrsocketdemo;

import java.util.function.Function;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.rsocket.RSocketRequester;

@SpringBootApplication
@EnableConfigurationProperties(RSocketServerProperties.class)
public class MultibinderRsocketDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(MultibinderRsocketDemoApplication.class, args);
	}

	@Bean
	public Function<String, String> process(RSocketRequester rSocketRequester) {
		return payload -> rSocketRequester
				.route("uppercase")
				.data(payload)
				.retrieveMono(String.class)
				.block();
	}

	@Bean
	public RSocketRequester rSocketRequester(RSocketRequester.Builder rsocketRequesterBuilder, RSocketServerProperties rSocketServerProperties) {
		return rsocketRequesterBuilder.tcp(rSocketServerProperties.getIp(), rSocketServerProperties.getPort());
	}
}
