package com.kuuhaku.handlers.api.websocket;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.kuuhaku.Main;
import com.kuuhaku.controller.mysql.GlobalMessageDAO;
import com.kuuhaku.model.GlobalMessage;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONObject;

public class WebSocketConfig {

	private final SocketIOServer socket;

	public WebSocketConfig() {
		Thread.currentThread().setName("chat-websocket");
		Configuration config = new Configuration();
		config.setHostname("localhost");
		config.setPort(8000);

		socket = new SocketIOServer(config);
		socket.addEventListener("chatevent", JSONObject.class, (client, data, ackSender) -> {
			System.out.println(data);

			User u = Main.getInfo().getUserByID(data.getString("userID"));

			Helper.logger(this.getClass()).info("Mensagem enviada por " + u.getName() + ": " + data.getString("content"));

			GlobalMessage gm = new GlobalMessage();

			gm.setUserId(u.getId());
			gm.setName(u.getName());
			gm.setAvatar(u.getAvatarUrl());
			gm.setContent(data.getString("content"));

			GlobalMessageDAO.saveMessage(gm);

			Main.getRelay().relayMessage(gm);

			socket.getBroadcastOperations().sendEvent("chat", gm.toString());
		});
		socket.start();
	}

	public SocketIOServer getSocket() {
		return socket;
	}
}
