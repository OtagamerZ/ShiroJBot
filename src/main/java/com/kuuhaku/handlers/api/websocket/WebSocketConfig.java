package com.kuuhaku.handlers.api.websocket;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.kuuhaku.utils.Helper;

public class WebSocketConfig {

	private final SocketIOServer socket;

	private WebSocketConfig() {
		Configuration config = new Configuration();
		config.setHostname("164.68.110.221");
		config.setPort(3000);

		socket = new SocketIOServer(config);
		socket.addEventListener("chatevent", String.class, (client, data, ackSender) -> System.out.println(data));
		socket.start();
		Helper.logger(this.getClass()).info("Socket conectado com sucesso!");
	}

	public static SocketIOServer getSocket() {
		return new WebSocketConfig().socket;
	}
}
