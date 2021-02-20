/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.handlers.api.websocket;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.CanvasDAO;
import com.kuuhaku.controller.postgresql.TokenDAO;
import com.kuuhaku.model.persistent.PixelCanvas;
import com.kuuhaku.model.persistent.PixelOperation;
import com.kuuhaku.model.persistent.Token;
import com.kuuhaku.utils.Helper;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.awt.*;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

public class CanvasSocket extends WebSocketServer {
	private final Set<WebSocket> clients = new HashSet<>();

	public CanvasSocket(InetSocketAddress address) {
		super(address);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		clients.add(conn);
		conn.send(new JSONObject() {{
			put("type", "canvas_init");
			put("content", Main.getInfo().getCanvas().getRawCanvas());
			put("info", new JSONObject() {{
				put("size", Helper.CANVAS_SIZE);
			}});
		}}.toString());
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		clients.remove(conn);
	}

	@Override
	public void onMessage(WebSocket conn, String payload) {
		JSONObject jo = new JSONObject(payload);
		if (!jo.has("token") || !jo.has("type") || !jo.has("content") || !TokenDAO.validateToken(jo.getString("token")))
			return;

		switch (jo.getString("type")) {
			case "canvas" -> {
				JSONObject pixel = jo.getJSONObject("content").getJSONObject("pixel");
				Token t = TokenDAO.getToken(jo.getString("token"));
				if (t == null) return;
				try {
					PixelOperation op = new PixelOperation(
							jo.getString("token"),
							t.getHolder(),
							pixel.getInt("x"),
							pixel.getInt("y"),
							pixel.getString("color")
					);

					CanvasDAO.saveOperation(op);
				} catch (NullPointerException e) {
					return;
				}

				String color = pixel.getString("color");
				int x = pixel.getInt("x");
				int y = pixel.getInt("y");
				PixelCanvas.addPixel(new int[]{x, y}, Color.decode(color));
				notifyUpdate(color, x, y);
			}
			case "chat" -> {
				for (WebSocket s : clients) {
					s.send(new JSONObject() {{
						put("type", "chat");
						put("content", jo.getJSONObject("content"));
					}}.toString());
				}
			}
		}
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {

	}

	@Override
	public void onStart() {
		Helper.logger(this.getClass()).info("WebSocket \"canvas\" iniciado na porta " + this.getPort());
	}

	public void notifyUpdate(String color, int x, int y) {
		for (WebSocket s : clients) {
			s.send(new JSONObject() {{
				put("type", "canvas_update");
				put("color", color);
				put("x", x);
				put("y", y);
				put("info", new JSONObject() {{
					put("size", Helper.CANVAS_SIZE);
				}});
			}}.toString());
		}
	}
}
