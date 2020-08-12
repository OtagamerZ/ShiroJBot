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
import com.github.twitch4j.chat.TwitchChat;
import com.github.twitch4j.graphql.TwitchGraphQL;
import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.kraken.TwitchKraken;
import com.github.twitch4j.pubsub.TwitchPubSub;
import com.github.twitch4j.tmi.TwitchMessagingInterface;

import java.util.concurrent.ScheduledThreadPoolExecutor;

public class TwitchClient extends com.github.twitch4j.TwitchClient {
	EventManager events = new EventManager();

	/**
	 * Constructor
	 *
	 * @param eventManager       EventManager
	 * @param helix              TwitchHelix
	 * @param kraken             TwitchKraken
	 * @param messagingInterface TwitchMessagingInterface
	 * @param chat               TwitchChat
	 * @param pubsub             TwitchPubSub
	 * @param graphql            TwitchGraphQL
	 * @param threadPoolExecutor ScheduledThreadPoolExecutor
	 */
	public TwitchClient(EventManager eventManager, TwitchHelix helix, TwitchKraken kraken, TwitchMessagingInterface messagingInterface, TwitchChat chat, TwitchPubSub pubsub, TwitchGraphQL graphql, ScheduledThreadPoolExecutor threadPoolExecutor) {
		super(eventManager, helix, kraken, messagingInterface, chat, pubsub, graphql, threadPoolExecutor);
		events.autoDiscovery();
	}

	public void addEventListener(Object listener) {
		events.getEventHandler(SimpleEventHandler.class).registerListener(listener);
	}
}
