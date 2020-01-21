package com.kuuhaku.handlers.api.websocket;

public class Message {
	private String name;
	private String content;

	public Message() {
	}

	public Message(String name, String content) {
		this.name = name;
		this.content = content;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}
