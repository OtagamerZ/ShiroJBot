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

package com.kuuhaku.game.engine;

import com.kuuhaku.Constants;
import com.kuuhaku.listener.GuildListener;
import com.kuuhaku.model.common.GameChannel;
import com.kuuhaku.model.common.PatternCache;
import com.kuuhaku.model.common.SimpleMessageListener;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.records.shoukan.HistoryLog;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public abstract class GameInstance<T extends Enum<T>> {
	public static final Set<String> PLAYERS = ConcurrentHashMap.newKeySet();
	private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

	private CompletableFuture<Void> exec;
	private DelayedAction timeout;
	private GameChannel channel;
	private int turn = 1;
	private T phase;
	private boolean initialized;

	private final I18N locale;
	private final String[] players;
	private final Deque<HistoryLog> history = new ArrayDeque<>();

	public GameInstance(I18N locale, String[] players) {
		this.locale = locale;
		this.players = players;
	}

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
						try {
							runtime(event.getAuthor(), event.getMessage().getContentRaw());
						} catch (InvocationTargetException | IllegalAccessException e) {
							Constants.LOGGER.error(e, e);
						}
					}
				}
			};

			try {
				begin();
				GuildListener.addHandler(guild, sml);
				initialized = true;
				timeout.start();

				while (!exec.isDone()) Thread.onSpinWait();
			} catch (GameReport e) {
				initialized = true;
				//noinspection MagicConstant
				close(e.getCode());
			} catch (Exception e) {
				initialized = true;
				close(GameReport.INITIALIZATION_ERROR);
				Constants.LOGGER.error(e, e);
			} finally {
				sml.close();
			}
		});
	}

	protected abstract boolean validate(Message message);

	protected abstract void begin();

	protected abstract void runtime(User user, String value) throws InvocationTargetException, IllegalAccessException;

	public I18N getLocale() {
		return locale;
	}

	public String[] getPlayers() {
		return players;
	}

	public void setTimeout(Consumer<Integer> action, int time, TimeUnit unit) {
		if (timeout != null) {
			timeout.stop();
		}

		this.timeout = DelayedAction.of(service)
				.setTimeUnit(time, unit)
				.setTask(() -> action.accept(turn));
	}

	public GameChannel getChannel() {
		return channel;
	}

	public int getTurn() {
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

	public T getPhase() {
		return phase;
	}

	public void setPhase(T phase) {
		this.phase = phase;
	}

	public boolean isInitialized() {
		return initialized;
	}

	public Deque<HistoryLog> getHistory() {
		return history;
	}

	protected Pair<Method, JSONObject> toAction(String args) {
		Method[] meths = getClass().getDeclaredMethods();
		for (Method meth : meths) {
			PlayerAction pa = meth.getAnnotation(PlayerAction.class);
			if (pa != null) {
				PhaseConstraint pc = meth.getAnnotation(PhaseConstraint.class);
				if (pc != null && (phase == null || !Utils.equalsAny(phase.name(), pc.value()))) {
					continue;
				}

				Pattern pat = PatternCache.compile(pa.value());
				if (Utils.regex(args, pat).matches()) {
					return new Pair<>(meth, Utils.extractNamedGroups(args, pat));
				}
			}
		}

		return null;
	}

	public final boolean isClosed() {
		return exec.isDone();
	}

	public final void close(@MagicConstant(valuesFromClass = GameReport.class) byte code) {
		timeout.stop();
		if (code == GameReport.SUCCESS) {
			exec.complete(null);
		} else {
			exec.completeExceptionally(new GameReport(code));
		}

		channel = null;
	}
}
