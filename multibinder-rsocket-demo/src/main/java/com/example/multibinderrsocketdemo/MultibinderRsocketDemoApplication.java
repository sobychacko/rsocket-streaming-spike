package com.example.multibinderrsocketdemo;

import java.util.function.Function;

import reactor.core.publisher.Flux;

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
	public Function<Flux<byte[]>, Flux<byte[]>> proxy(RSocketRequester rSocketRequester,
													  RSocketServerProperties rSocketServerProperties) {
		return payload -> rSocketRequester
				.route(rSocketServerProperties.getRoute())
				.data(payload)
				.retrieveFlux(byte[].class);
	}

	@Bean
	public RSocketRequester rSocketRequester(RSocketRequester.Builder rsocketRequesterBuilder, RSocketServerProperties rSocketServerProperties) {
		return rsocketRequesterBuilder.tcp(rSocketServerProperties.getIp(), rSocketServerProperties.getPort());
	}
}
