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

package com.kuuhaku.events;

import com.github.philippheuer.events4j.simple.SimpleEventHandler;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.chat.events.channel.FollowEvent;
import com.github.twitch4j.events.ChannelGoLiveEvent;

public class TwitchEvents {
	private final SimpleEventHandler handler;

	public TwitchEvents(TwitchClient client) {
		this.handler = client.getEventManager().getEventHandler(SimpleEventHandler.class);
		handler.onEvent(ChannelMessageEvent.class, this::onChannelMessageEvent);
		handler.onEvent(FollowEvent.class, this::onFollowEvent);
		handler.onEvent(ChannelGoLiveEvent.class, this::onChannelGoLiveEvent);
	}

	private void onChannelMessageEvent(ChannelMessageEvent evt) {
		System.out.println("Message received: " + evt.getMessage());
	}

	private void onFollowEvent(FollowEvent evt) {

	}

	private void onChannelGoLiveEvent(ChannelGoLiveEvent evt) {

	}

	public SimpleEventHandler getHandler() {
		return handler;
	}
}
