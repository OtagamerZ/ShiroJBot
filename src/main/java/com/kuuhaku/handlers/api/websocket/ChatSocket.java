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
import org.json.JSONObject;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.ArrayList;
import java.util.List;

@ServerEndpoint("/chat/{uid}")
public class ChatSocket {
	List<Session> clients = new ArrayList<>();

	@OnOpen
	public void onOpen(Session session) {
		clients.add(session);
	}

	@OnClose
	public void onClose(Session session) {
		clients.remove(session);
	}

	@OnMessage
	public void onMessage(String payload, @PathParam("uid") String uid) {
		JSONObject data = new JSONObject(payload);
		User u = Main.getInfo().getUserByID(uid);

		Helper.logger(this.getClass()).debug("Mensagem enviada por " + u.getName() + ": " + data.getString("content"));

		GlobalMessage gm = new GlobalMessage();

		gm.setUserId(u.getId());
		gm.setName(u.getName());
		gm.setAvatar(u.getAvatarUrl());
		gm.setContent(data.getString("content"));

		GlobalMessageDAO.saveMessage(gm);

		Main.getRelay().relayMessage(gm);

		clients.forEach(s -> s.getAsyncRemote().sendText(payload));
	}
}
