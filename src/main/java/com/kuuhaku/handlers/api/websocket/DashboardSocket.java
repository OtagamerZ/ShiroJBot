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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.kuuhaku.handlers.api.endpoint.ReadyData;
import com.kuuhaku.utils.Helper;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class DashboardSocket extends WebSocketServer {
	private final Cache<String, BiContract<WebSocket, ReadyData>> requests = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();

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
		JSONObject jo = new JSONObject(message);
		if (!jo.has("type") || !jo.getString("type").equals("login")) return;

		BiContract<WebSocket, ReadyData> request = requests.getIfPresent(jo.getString("data"));
		if (request == null) request = new BiContract<>((ws, data) -> ws.send(data.getData().toString()));
		request.setSignatureA(conn);
		requests.put(jo.getString("data"), request);
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
	}

	@Override
	public void onStart() {
		Helper.logger(this.getClass()).info("WebSocket \"dashboard\" iniciado na porta " + this.getPort());
	}

	public void addReadyData(ReadyData rdata, String session) {
		BiContract<WebSocket, ReadyData> request = requests.getIfPresent(session);
		if (request == null) request = new BiContract<>((ws, data) -> ws.send(data.getData().toString()));
		request.setSignatureB(rdata);
		requests.put(session, request);
	}

	public Cache<String, BiContract<WebSocket, ReadyData>> getRequests() {
		return requests;
	}
}
