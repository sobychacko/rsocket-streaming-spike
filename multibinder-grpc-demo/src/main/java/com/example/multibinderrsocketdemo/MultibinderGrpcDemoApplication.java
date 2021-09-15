package com.example.multibinderrsocketdemo;

import java.util.function.Function;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.function.grpc.FunctionGrpcProperties;
import org.springframework.cloud.function.grpc.GrpcUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;

@SpringBootApplication
@EnableConfigurationProperties(FunctionGrpcProperties.class)
public class MultibinderGrpcDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(MultibinderGrpcDemoApplication.class, args);
	}

	@Bean
	public Function<Message<byte[]>, Message<byte[]>> proxy(FunctionGrpcProperties properties) {
		return message -> GrpcUtils.requestReply("localhost", properties.getPort(), message);
	}

}
