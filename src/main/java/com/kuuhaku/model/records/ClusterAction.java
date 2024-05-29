/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.records;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public record ClusterAction(long delay, Map<String, MessageCreateAction> actions) {
	public ClusterAction embed(MessageEmbed eb) {
		actions.replaceAll((k, msg) -> msg.setEmbeds(eb));
		return this;
	}

	public ClusterAction addFile(byte[] bytes, String filename) {
		actions.replaceAll((k, msg) -> msg.addFiles(FileUpload.fromData(bytes, filename)));
		return this;
	}

	public ClusterAction apply(Function<MessageCreateAction, MessageCreateAction> action) {
		actions.replaceAll((k, msg) -> action.apply(msg));
		return this;
	}

	public void queue() {
		queue(null, null);
	}

	public void queue(Consumer<? super Message> message) {
		queue(message, null);
	}

	public void queue(Consumer<? super Message> message, Consumer<? super Throwable> failure) {
		for (MessageCreateAction act : actions.values()) {
			act.delay(delay, TimeUnit.MILLISECONDS).queue(message, failure);
		}
	}

	public void queueAfter(long delay, TimeUnit unit) {
		queueAfter(delay, unit, null, null);
	}

	public void queueAfter(long delay, TimeUnit unit, Consumer<? super Message> message) {
		queueAfter(delay, unit, message, null);
	}

	public void queueAfter(long delay, TimeUnit unit, Consumer<? super Message> message, Consumer<? super Throwable> failure) {
		for (MessageCreateAction act : actions.values()) {
			act.delay(delay, TimeUnit.MILLISECONDS).queueAfter(delay, unit, message, failure);
		}
	}
}