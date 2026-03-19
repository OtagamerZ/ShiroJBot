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

import com.kuuhaku.Constants;
import com.kuuhaku.util.Utils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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

	public ClusterAction addFile(File file) {
		if (file == null) return this;

		actions.replaceAll((k, msg) -> msg.addFiles(FileUpload.fromData(file)));
		return this;
	}

	public ClusterAction apply(Function<MessageCreateAction, MessageCreateAction> action) {
		actions.replaceAll((k, msg) -> action.apply(msg));
		return this;
	}

	public CompletableFuture<Void> queue() {
		return queue(null);
	}

	public CompletableFuture<Void> queue(Consumer<? super Message> success) {
		return queue(success, Utils::doNothing);
	}

	public CompletableFuture<Void> queue(Consumer<? super Message> success, Consumer<? super Throwable> failure) {
		Iterator<Map.Entry<String, MessageCreateAction>> it = actions.entrySet().iterator();
		List<CompletableFuture<Message>> futures = new ArrayList<>();
		while (it.hasNext()) {
			Map.Entry<String, MessageCreateAction> e = it.next();

			try {
				RestAction<Message> act = e.getValue();
				if (delay > 0) {
					act = act.delay(delay, TimeUnit.MILLISECONDS);
				}

				futures.add(act.submit()
						.whenComplete((msg, t) -> {
							if (msg != null) {
								success.accept(msg);
							} else if (t != null) {
								failure.accept(t);
							}
						})
				);
			} catch (Exception ex) {
				Constants.LOGGER.error("Failed to queue action for channel {}", e.getKey(), ex);
			}

			it.remove();
		}

		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
	}

	public CompletableFuture<Void> queueAfter(long delay, TimeUnit unit) {
		return queueAfter(delay, unit, null);
	}

	public CompletableFuture<Void> queueAfter(long delay, TimeUnit unit, Consumer<? super Message> success) {
		return queueAfter(delay, unit, success, Utils::doNothing);
	}

	public CompletableFuture<Void> queueAfter(long delay, TimeUnit unit, Consumer<? super Message> success, Consumer<? super Throwable> failure) {
		Iterator<Map.Entry<String, MessageCreateAction>> it = actions.entrySet().iterator();
		List<CompletableFuture<Message>> futures = new ArrayList<>();
		while (it.hasNext()) {
			Map.Entry<String, MessageCreateAction> e = it.next();

			try {
				RestAction<Message> act = e.getValue();
				if (delay > 0) {
					act = act.delay(this.delay, TimeUnit.MILLISECONDS);
				}

				futures.add(act.submitAfter(delay, unit)
						.whenComplete((msg, t) -> {
							if (msg != null) {
								success.accept(msg);
							} else if (t != null) {
								failure.accept(t);
							}
						})
				);
			} catch (Exception ex) {
				Constants.LOGGER.error("Failed to queue action for channel {}", e.getKey(), ex);
			}

			it.remove();
		}

		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
	}
}