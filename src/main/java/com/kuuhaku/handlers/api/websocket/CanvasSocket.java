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
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		clients.remove(conn);
	}

	@Override
	public void onMessage(WebSocket conn, String payload) {
		JSONObject jo = new JSONObject(payload);
		if (!jo.has("pixel") || !jo.has("token")) return;
		else if (!TokenDAO.validateToken(jo.getString("token"))) return;

		JSONObject pixel = jo.getJSONObject("pixel");

		PixelCanvas canvas = Main.getInfo().getCanvas();
		canvas.addPixel(null, new int[]{pixel.getInt("x"), pixel.getInt("y")}, Color.decode(pixel.getString("color")));

		CanvasDAO.saveCanvas(canvas);

		notifyUpdate();
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {

	}

	@Override
	public void onStart() {
		Helper.logger(this.getClass()).info("WebSocket \"canvas\" iniciado na porta " + this.getPort());
	}

	public void notifyUpdate() {
		clients.forEach(s -> s.send(Main.getInfo().getCanvas().getRawCanvas()));
	}
}
