package com.kuuhaku.handlers.api.websocket;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;

public class WebSocketConfig {

	private final SocketIOServer socket;

	private WebSocketConfig() {
		Configuration config = new Configuration();
		config.setHostname("localhost");
		config.setPort(8000);

		socket = new SocketIOServer(config);
		socket.addEventListener("chatevent", String.class, (client, data, ackSender) -> System.out.println(data));
		socket.start();
	}

	public static SocketIOServer getSocket() {
		return new WebSocketConfig().socket;
	}
}
