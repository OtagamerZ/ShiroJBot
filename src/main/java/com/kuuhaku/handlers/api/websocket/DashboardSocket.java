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

import com.kuuhaku.handlers.api.endpoint.ReadyData;
import com.kuuhaku.utils.Helper;

import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ServerEndpoint("/socket/require")
public class DashboardSocket {
	private final List<ReadyData> authQueue = new ArrayList<>();

	@OnMessage
	public void onMessage(String message, Session session) {
		authQueue.stream().filter(rd -> rd.getSessionId().equalsIgnoreCase(message)).findFirst().ifPresent(rd -> {
			try {
				session.getBasicRemote().sendText(rd.getData().toString());
				authQueue.remove(rd);
			} catch (IOException e) {
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		});
	}

	public void queue(ReadyData data) {
		authQueue.add(data);
	}

	public void sweep() {
		authQueue.removeIf(r -> TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()) - TimeUnit.MILLISECONDS.toMinutes(r.getCreatedAt()) >= 5);
	}
}
