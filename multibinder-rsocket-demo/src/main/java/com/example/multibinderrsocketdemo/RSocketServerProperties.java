package com.example.multibinderrsocketdemo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("rsocket.server")
public class RSocketServerProperties {

	private String ip = "localhost";

	private Integer port = 7000;

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}
}
