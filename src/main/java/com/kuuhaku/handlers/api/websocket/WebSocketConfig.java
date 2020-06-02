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

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.GlobalMessageDAO;
import com.kuuhaku.handlers.api.endpoint.ReadyData;
import com.kuuhaku.model.persistent.GlobalMessage;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONObject;

import java.net.BindException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WebSocketConfig {
	private final List<ReadyData> authQueue = new ArrayList<>();
	private final SocketIOServer socket;

	public WebSocketConfig(int port) throws BindException {
		Configuration config = new Configuration();
		config.setHostname("127.0.0.1");
		config.setPort(port);

		socket = new SocketIOServer(config);
		socket.addEventListener("chatevent", JSONObject.class, (client, data, ackSender) -> {
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
		socket.addEventListener("require", String.class, (client, data, ackSender) ->
				authQueue.stream().filter(rd -> rd.getSessionId().equalsIgnoreCase(data)).findFirst().ifPresent(rd -> {
					Main.getInfo().getServer().getSocket().getBroadcastOperations().sendEvent("auth_" + data, rd.getData().toString());
					authQueue.remove(rd);
				}));
		socket.start();
	}

	public SocketIOServer getSocket() {
		return socket;
	}

	public void queue(ReadyData data) {
		authQueue.add(data);
	}

	public void sweep() {
		authQueue.removeIf(r -> TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()) - TimeUnit.MILLISECONDS.toMinutes(r.getCreatedAt()) >= 5);
	}
}
