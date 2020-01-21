package com.kuuhaku.handlers.api.websocket;

import com.kuuhaku.utils.Helper;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketConfig extends Thread {
	private final Map<WsContext, String> clients = new ConcurrentHashMap<>();

	public WebSocketConfig() {
		Thread.currentThread().setName("chat-websocket");
		Helper.logger(this.getClass()).info("WebSocket conectado na porta 3000");
		Javalin app = Javalin.create().start(3000);

		app.ws("/chat", ws -> {
			ws.onConnect(ctx -> {
				clients.put(ctx, "User " + System.currentTimeMillis());

			});
			ws.onClose(clients::remove);
			ws.onMessage(System.out::println);
		});
	}
}
