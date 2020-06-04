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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class WebSocketConfig {
	private final ChatSocket chat;
	private final DashboardSocket dashboard = new DashboardSocket();
	private final Map<Integer, Boolean> ports = new HashMap<>() {{
		put(8001, false);
		put(8002, false);
		put(8003, false);
		put(8004, false);
	}};

	public WebSocketConfig() {
		for (Map.Entry<Integer, Boolean> e : ports.entrySet()) {
			try {
				new Socket("localhost", e.getKey());
				ports.put(e.getKey(), true);
			} catch (IOException ex) {
				ports.put(e.getKey(), false);
			}
		}

		LinkedList<Integer> available = ports.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toCollection(LinkedList::new));

		this.chat = new ChatSocket(new InetSocketAddress(Objects.requireNonNull(available.poll())));
		Executors.newSingleThreadExecutor().execute(chat::start);
	}

	public ChatSocket getChat() {
		return chat;
	}

	public DashboardSocket getDashboard() {
		return dashboard;
	}
}
