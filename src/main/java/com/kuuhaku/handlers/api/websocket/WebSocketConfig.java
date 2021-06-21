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

import com.kuuhaku.utils.Helper;

import java.io.IOException;
import java.net.InetSocketAddress;

public class WebSocketConfig {
	private final DashboardSocket dashboard;
	private final CanvasSocket canvas;

	public WebSocketConfig() {
		dashboard = new DashboardSocket(new InetSocketAddress(8001));
		canvas = new CanvasSocket(new InetSocketAddress(8002));

		dashboard.setReuseAddr(true);
		canvas.setReuseAddr(true);

		dashboard.start();
		canvas.start();
	}

	public DashboardSocket getDashboard() {
		return dashboard;
	}

	public CanvasSocket getCanvas() {
		return canvas;
	}

	public void shutdown() {
		try {
			dashboard.stop();
			canvas.stop();
		} catch (IOException | InterruptedException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}
}
