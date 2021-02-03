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

package com.kuuhaku.handlers.games.tabletop;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ClusterAction {
	private final List<MessageAction> actions;

	public ClusterAction(List<MessageAction> actions) {
		this.actions = actions;
	}

	public ClusterAction embed(MessageEmbed eb) {
		actions.replaceAll(msg -> msg.embed(eb));
		return this;
	}

	public ClusterAction addFile(byte[] bytes, String filename) {
		actions.replaceAll(msg -> msg.addFile(bytes, filename));
		return this;
	}

	public void queue(Consumer<? super Message> message) {
		for (MessageAction act : actions) {
			act.queue(message);
		}
	}

	public void queue(Consumer<? super Message> message, Consumer<? super Throwable> failure) {
		for (MessageAction act : actions) {
			act.queue(message, failure);
		}
	}

	public void queueAfter(long delay, TimeUnit unit, Consumer<? super Message> message) {
		for (MessageAction act : actions) {
			act.queueAfter(delay, unit, message);
		}
	}

	public void queueAfter(long delay, TimeUnit unit, Consumer<? super Message> message, Consumer<? super Throwable> failure) {
		for (MessageAction act : actions) {
			act.queueAfter(delay, unit, message, failure);
		}
	}
}
