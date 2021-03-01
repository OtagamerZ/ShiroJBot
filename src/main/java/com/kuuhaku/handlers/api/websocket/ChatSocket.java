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
import com.kuuhaku.controller.postgresql.GlobalMessageDAO;
import com.kuuhaku.model.persistent.GlobalMessage;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.User;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

public class ChatSocket extends WebSocketServer {
	private final Set<WebSocket> clients = new HashSet<>();

	public ChatSocket(InetSocketAddress address) {
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
		JSONObject data = new JSONObject(payload);
		User u = Main.getInfo().getUserByID(data.getString("userId"));

		Helper.logger(this.getClass()).debug("Mensagem enviada por " + u.getName() + ": " + data.getString("content"));

		GlobalMessage gm = new GlobalMessage();

		gm.setUid(u.getId());
		gm.setName(u.getName());
		gm.setAvatar(u.getEffectiveAvatarUrl());
		gm.setContent(data.getString("content"));

		GlobalMessageDAO.saveMessage(gm);

		if (conn != null) {
			Main.getRelay().relayMessage(gm);
		}
		for (WebSocket s : clients) {
			s.send(payload);
		}
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {

	}

	@Override
	public void onStart() {
		Helper.logger(this.getClass()).info("WebSocket \"chat\" iniciado na porta " + this.getPort());
	}
}
