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

package com.kuuhaku.model.common;

import com.github.philippheuer.events4j.core.EventManager;
import com.github.philippheuer.events4j.simple.SimpleEventHandler;

public class TwitchClient {
	private final EventManager events;
	private final com.github.twitch4j.TwitchClient client;

	public TwitchClient(com.github.twitch4j.TwitchClient client) {
		this.client = client;
		this.events = client.getEventManager();
		events.autoDiscovery();
	}

	public void addEventListener(Object listener) {
		events.getEventHandler(SimpleEventHandler.class).registerListener(listener);
	}

	public com.github.twitch4j.TwitchClient getClient() {
		return client;
	}
}
