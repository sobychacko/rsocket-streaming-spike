package com.example.uppercasersocketdemo;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.util.SocketUtils;

@SpringBootTest
class UppercaseRsocketDemoApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	public void testWithRouteAndDefinition() {
		int port = SocketUtils.findAvailableTcpPort();
		try (
				ConfigurableApplicationContext applicationContext =
						new SpringApplicationBuilder(UppercaseRsocketDemoApplication.class)
								.web(WebApplicationType.NONE)
								.run("--logging.level.org.springframework.cloud.function=DEBUG",
										"--spring.cloud.function.definition=uppercase",
										"--spring.rsocket.server.port=" + port);
		) {
			RSocketRequester.Builder rsocketRequesterBuilder =
					applicationContext.getBean(RSocketRequester.Builder.class);

			rsocketRequesterBuilder.tcp("localhost", port)
					.route("uppercase")
					.data("hello")
					.retrieveMono(String.class)
					.as(StepVerifier::create)
					.expectNext("HELLO")
					.expectComplete()
					.verify();
		}
	}

}
