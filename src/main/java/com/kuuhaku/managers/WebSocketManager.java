/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.managers;

import com.kuuhaku.handlers.api.websocket.CanvasSocket;
import com.kuuhaku.utils.helpers.MiscHelper;

import java.io.IOException;
import java.net.InetSocketAddress;

public class WebSocketManager {
	private final CanvasSocket canvas;

	public WebSocketManager() {
		canvas = new CanvasSocket(new InetSocketAddress(8002));
		canvas.setReuseAddr(true);

		canvas.start();
	}

	public CanvasSocket getCanvas() {
		return canvas;
	}

	public void shutdown() {
		try {
			canvas.stop();
		} catch (InterruptedException | IOException e) {
			MiscHelper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}
}
