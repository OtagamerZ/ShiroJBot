package com.kuuhaku.handlers.api.websocket;

public class Message {
	private String content;

	public Message() {
	}

	public Message(String content) {
		this.content = content;
	}

	public String getContent() {
		return content;
	}
}
