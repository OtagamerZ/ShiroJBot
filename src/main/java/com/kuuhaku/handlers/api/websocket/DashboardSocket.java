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
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DashboardSocket extends WebSocketServer {
	private final List<ReadyData> authQueue = new ArrayList<>();

	public DashboardSocket(InetSocketAddress address) {
		super(address);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		authQueue.stream().filter(rd -> rd.getSessionId().equalsIgnoreCase(message)).findFirst().ifPresent(rd -> {
			conn.send(rd.getData().toString());
			authQueue.remove(rd);
		});
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
	}

	@Override
	public void onStart() {
	}

	public void queue(ReadyData data) {
		authQueue.add(data);
	}

	public void sweep() {
		authQueue.removeIf(r -> TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis()) - TimeUnit.MILLISECONDS.toMinutes(r.getCreatedAt()) >= 5);
	}
}
