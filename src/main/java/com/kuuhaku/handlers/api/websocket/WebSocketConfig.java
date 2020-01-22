package com.kuuhaku.handlers.api.websocket;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.kuuhaku.Main;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONObject;

import java.util.concurrent.Executors;

public class WebSocketConfig {

	private SocketIOServer socket;

	private WebSocketConfig() {
		Executors.newSingleThreadExecutor().execute(() -> {
			Thread.currentThread().setName("chat-websocket");
			Configuration config = new Configuration();
			config.setHostname("localhost");
			config.setPort(8000);

			socket = new SocketIOServer(config);
			socket.addEventListener("chatevent", Object.class, (client, data, ackSender) -> {
				JSONObject jo = new JSONObject(data);

				User u = Main.getInfo().getUserByID((String) jo.get("userID"));

				System.out.println("Mensagem enviada por " + u.getName() + ": " + jo.get("content"));

				JSONObject out = new JSONObject();

				out.put("id", u.getId());
				out.put("name", u.getName());
				out.put("avatar", u.getAvatarUrl());
				out.put("content", jo.get("content"));

				socket.getBroadcastOperations().sendEvent("chat", out.toString());
			});
			socket.start();
		});
	}

	public static SocketIOServer getSocket() {
		return new WebSocketConfig().socket;
	}
}
