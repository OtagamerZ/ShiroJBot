package com.kuuhaku.handlers.api.websocket;

public class Sender {
	private String name;

	public Sender() {
	}

	public Sender(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
