/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

public record ClusterAction(Map<String, MessageCreateAction> actions) {
	public ClusterAction embed(MessageEmbed eb) {
		actions.replaceAll((k, msg) -> msg.setEmbeds(eb));
		return this;
	}

	public ClusterAction addFile(byte[] bytes, String filename) {
		actions.replaceAll((k, msg) -> msg.addFiles(FileUpload.fromData(bytes, filename)));
		return this;
	}

	public void queue() {
		for (MessageCreateAction act : actions.values()) {
			act.queue();
		}
	}

	public void queue(Consumer<? super Message> message) {
		for (MessageCreateAction act : actions.values()) {
			act.queue(message);
		}
	}

	public void queue(Consumer<? super Message> message, Consumer<? super Throwable> failure) {
		for (MessageCreateAction act : actions.values()) {
			act.queue(message, failure);
		}
	}

	public void queueAfter(long delay, TimeUnit unit, Consumer<? super Message> message) {
		for (MessageCreateAction act : actions.values()) {
			act.queueAfter(delay, unit, message);
		}
	}

	public void queueAfter(long delay, TimeUnit unit, Consumer<? super Message> message, Consumer<? super Throwable> failure) {
		for (MessageCreateAction act : actions.values()) {
			act.queueAfter(delay, unit, message, failure);
		}
	}
}