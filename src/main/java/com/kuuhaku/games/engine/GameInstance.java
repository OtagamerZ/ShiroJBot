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

package com.kuuhaku.games.engine;

import com.kuuhaku.listeners.GuildListener;
import com.kuuhaku.model.common.GameChannel;
import com.kuuhaku.model.common.SimpleMessageListener;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class GameInstance {
	private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

	private CompletableFuture<Void> exec;
	private DelayedAction timeout;
	private GameChannel channel;
	private int turn = 1;

	public final CompletableFuture<Void> start(Guild guild, TextChannel... channels) {
		return exec = CompletableFuture.runAsync(() -> {
			SimpleMessageListener sml = new SimpleMessageListener(channels) {
				{
					turn = 1;
					channel = getChannel();
				}

				@Override
				public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
					if (checkChannel(event.getChannel()) && validate(event.getMessage())) {
						runtime(event.getMessage().getContentRaw());
					}
				}
			};

			begin();
			GuildListener.addHandler(guild, sml);
			while (!exec.isDone()) Thread.onSpinWait();

			sml.close();
		});
	}

	protected abstract void begin();

	protected abstract void runtime(String value);

	protected abstract boolean validate(Message message);

	public void setTimeout(Consumer<Integer> action, int time, TimeUnit unit) {
		this.timeout = DelayedAction.of(service)
				.setTimeUnit(time, unit)
				.setTask(() -> action.accept(turn));
	}

	public GameChannel getChannel() {
		return channel;
	}

	protected int getTurn() {
		return turn;
	}

	protected void nextTurn() {
		turn++;
		if (timeout != null) {
			timeout.restart();
		}
	}

	protected void resetTimer() {
		if (timeout != null) {
			timeout.restart();
		}
	}

	public final void close(int code) {
		if (code == 0) {
			exec.complete(null);
		} else {
			exec.completeExceptionally(new GameException(code));
		}

		channel = null;
	}
}
