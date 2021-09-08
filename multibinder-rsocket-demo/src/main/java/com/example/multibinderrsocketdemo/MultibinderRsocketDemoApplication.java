package com.example.multibinderrsocketdemo;

import java.util.function.Function;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.cloud.function.json.JsonMapper;
import org.springframework.cloud.function.rsocket.MessageAwareJsonDecoder;
import org.springframework.cloud.function.rsocket.MessageAwareJsonEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.rsocket.RSocketRequester;

@SpringBootApplication(excludeName = {"org.springframework.cloud.function.rsocket.RSocketAutoConfiguration",
					"org.springframework.cloud.function.rsocket.RSocketRoutingAutoConfiguration"})
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

	@Bean
	RSocketStrategiesCustomizer rSocketStrategiesCustomizer(JsonMapper jsonMapper) {
		return strategies -> strategies
				.encoders(encoders -> {
					encoders.add(0, new MessageAwareJsonEncoder(jsonMapper, true));
				})
				.decoders(decoders -> {
					decoders.add(0, new MessageAwareJsonDecoder(jsonMapper));
				});
	}
}
