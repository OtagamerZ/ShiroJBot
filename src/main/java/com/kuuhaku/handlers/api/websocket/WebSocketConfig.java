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

import java.net.InetSocketAddress;

public class WebSocketConfig {
	private final ChatSocket chat;
	private final DashboardSocket dashboard;
	private final CanvasSocket canvas;

	public WebSocketConfig() {
		chat = new ChatSocket(new InetSocketAddress(8001));
		dashboard = new DashboardSocket(new InetSocketAddress(8002));
		canvas = new CanvasSocket(new InetSocketAddress(8003));

		chat.start();
		dashboard.start();
		canvas.start();
	}

	public ChatSocket getChat() {
		return chat;
	}

	public DashboardSocket getDashboard() {
		return dashboard;
	}

	public CanvasSocket getCanvas() {
		return canvas;
	}
}
