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

package com.kuuhaku.game.engine;

import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.game.Shoukan;
import com.kuuhaku.listener.GuildListener;
import com.kuuhaku.model.common.GameChannel;
import com.kuuhaku.model.common.SimpleMessageListener;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.shoukan.HistoryLog;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public abstract class GameInstance<T extends Enum<T>> {
	public static final Set<String> CHANNELS = ConcurrentHashMap.newKeySet();
	public static final Set<String> PLAYERS = ConcurrentHashMap.newKeySet();

	private final ExecutorService worker = Executors.newSingleThreadExecutor();
	private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
	private final long seed = ThreadLocalRandom.current().nextLong();
	private final RandomGenerator rng = new SplittableRandom(seed);

	private CompletableFuture<Void> exec;
	private DelayedAction timeout;
	private GameChannel channel;
	private int turn = 1;
	private T phase;
	private boolean initialized;
	private int exitCode;

	private final I18N locale;
	private final String[] players;
	private final Deque<HistoryLog> history = new ArrayDeque<>();
	private String[] channels;

	public GameInstance(I18N locale, String[] players) {
		this.locale = locale;
		this.players = players;
	}

	public long getSeed() {
		return seed;
	}

	public final CompletableFuture<Void> start(Guild guild, GuildMessageChannel... chns) {
		SimpleMessageListener sml = new SimpleMessageListener(chns) {
			{
				turn = 1;
				channel = this.getChannel().setCooldown(1, TimeUnit.SECONDS);
			}

			@Override
			protected void onMessageReceived(@NotNull MessageReceivedEvent event) {
				if (checkChannel(event.getGuildChannel()) && validate(event.getMessage())) {
					try {
						runtime(event.getAuthor(), event.getMessage().getContentRaw());
					} catch (InvocationTargetException | IllegalAccessException e) {
						Constants.LOGGER.error(e, e);
					}
				}
			}
		};

		return exec = CompletableFuture.runAsync(() -> {
			try {
				channels = Stream.of(chns).map(GuildMessageChannel::getId).toArray(String[]::new);
				for (String chn : channels) {
					if (CHANNELS.contains(chn)) {
						channel.sendMessage(locale.get("error/channel_occupied_self")).queue();
						close(GameReport.INITIALIZATION_ERROR);
						return;
					}
				}

				PLAYERS.addAll(Arrays.asList(players));
				CHANNELS.addAll(Arrays.asList(channels));

				begin();
				GuildListener.addHandler(guild, sml);
				initialized = true;
				timeout.start();

				try {
					exec.join();
				} catch (Exception ignore) {
				}
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
				worker.close();
				service.close();

				for (String p : players) {
					PLAYERS.remove(p);
				}

				for (String c : channels) {
					CHANNELS.remove(c);
				}

				if (exitCode == GameReport.SUCCESS && getTurn() > 10) {
					if (this instanceof Shoukan s && !s.hasCheated() && s.getArcade() == null) {
						int prize = (int) (500 * Calc.rng(0.75, 1.25, s.getRng()));
						for (String uid : getPlayers()) {
							DAO.find(Account.class, uid).addCR(prize, getClass().getSimpleName());
						}
					} else if (!(this instanceof Shoukan)) {
						int prize = (int) (350 * Calc.rng(0.75, 1.25));
						for (String uid : getPlayers()) {
							DAO.find(Account.class, uid).addCR(prize, getClass().getSimpleName());
						}
					}
				}
			}
		}, worker);
	}

	protected abstract boolean validate(Message message);

	protected abstract void begin();

	protected abstract void runtime(User user, String value) throws InvocationTargetException, IllegalAccessException;

	public I18N getLocale() {
		return locale;
	}

	public String getString(String key, Object... args) {
		return locale.get(key, args);
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
		return toAction(args, m -> true);
	}

	protected Pair<Method, JSONObject> toAction(String args, Predicate<Method> condition) {
		Method[] meths = getClass().getDeclaredMethods();
		for (Method meth : meths) {
			PlayerAction pa = meth.getAnnotation(PlayerAction.class);
			if (pa != null) {
				PhaseConstraint pc = meth.getAnnotation(PhaseConstraint.class);
				if (pc != null && (phase == null || !Utils.equalsAny(phase.name(), pc.value()))) {
					continue;
				}

				Pattern pat = Main.getCacheManager().computePattern(pa.value(), (k, v) -> v == null ? Pattern.compile(pa.value()) : v);
				if (Utils.match(args, pat) && condition.test(meth)) {
					return new Pair<>(meth, Utils.extractNamedGroups(args, pat));
				}
			}
		}

		return null;
	}

	public RandomGenerator getRng() {
		return rng;
	}

	public final boolean isClosed() {
		return exec.isDone();
	}

	public final void close(@MagicConstant(valuesFromClass = GameReport.class) byte code) {
		timeout.stop();
		exitCode = code;

		if (code == GameReport.SUCCESS) {
			exec.complete(null);
		} else {
			exec.completeExceptionally(new GameReport(code));
		}
	}
}
