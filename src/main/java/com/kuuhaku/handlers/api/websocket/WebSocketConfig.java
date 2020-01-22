package com.kuuhaku.handlers.api.websocket;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;

public class WebSocketConfig {

	private final SocketIOServer server;

	private WebSocketConfig() {
		Configuration config = new Configuration();
		config.setHostname("164.68.110.221");
		config.setPort(8080);

		server = new SocketIOServer(config);
		server.addEventListener("chatevent", String.class, (client, data, ackSender) -> System.out.println(data));
	}

	public static SocketIOServer getServerSocket() {
		WebSocketConfig socket = new WebSocketConfig();
		socket.server.start();
		return socket.server;
	}
}
