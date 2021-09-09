package com.example.multibinderrsocketdemo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("rsocket.server")
public class RSocketServerProperties {

	private String ip = "localhost";

	private Integer port = 7000;

	private String route = "uppercase"; // default route

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

	public String getRoute() {
		return route;
	}

	public void setRoute(String route) {
		this.route = route;
	}
}
