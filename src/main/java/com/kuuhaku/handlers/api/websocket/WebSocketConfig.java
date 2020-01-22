package com.kuuhaku.handlers.api.websocket;

import io.socket.client.IO;
import io.socket.client.Socket;

import java.net.URISyntaxException;

public class WebSocketConfig {

	private final Socket socket;

	public WebSocketConfig() throws URISyntaxException {
		IO.Options options = new IO.Options();
		options.reconnection = true;
		Socket socket = IO.socket("http://164.68.110.221/chat");

		socket.on(Socket.EVENT_MESSAGE, System.out::println);
		this.socket = socket.connect();
	}

	public Socket getSocket() {
		return socket;
	}
}
