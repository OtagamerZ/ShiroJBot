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

package com.kuuhaku.game;

import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.game.engine.GameInstance;
import com.kuuhaku.game.engine.GameReport;
import com.kuuhaku.game.engine.NullPhase;
import com.kuuhaku.game.engine.PlayerAction;
import com.kuuhaku.model.common.InfiniteList;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.StringUtils;
import org.intellij.lang.annotations.MagicConstant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class Shiritori extends GameInstance<NullPhase> {
	private final long seed = Constants.DEFAULT_RNG.nextLong();

	private final I18N locale;
	private final String[] players;
	private final InfiniteList<String> inGame;
	private final File dict;
	private final Set<String> used = new HashSet<>();
	private Pair<String, String> message = null;
	private String current = null;

	public Shiritori(I18N locale, User... players) {
		this(locale, Arrays.stream(players).map(User::getId).toArray(String[]::new));
	}

	public Shiritori(I18N locale, String... players) {
		this.locale = locale;
		this.players = players;
		this.inGame = new InfiniteList<>(Set.of(players));
		this.dict = IO.getResourceAsFile("shiritori/" + locale.name().toLowerCase(Locale.ROOT) + ".dict");

		setTimeout(turn -> {
			getChannel().sendMessage(locale.get("str/game_wo_alt", "<@" + inGame.get() + ">", "<@" + inGame.peekNext() + ">")).queue();
			inGame.remove(inGame.get());

			if (inGame.size() == 1) {
				reportResult(GameReport.SUCCESS, "str/game_end_alt", inGame.get());
				return;
			}

			nextTurn();
		}, 1, TimeUnit.MINUTES);
	}

	@Override
	protected boolean validate(Message message) {
		return ((Predicate<Message>) m -> Utils.equalsAny(m.getAuthor().getId(), players))
				.and(m -> getTurn() % inGame.size() == inGame.indexOf(m.getAuthor().getId()))
				.test(message);
	}

	@Override
	protected void begin() {
		PLAYERS.addAll(Arrays.asList(players));
		reportEvent("str/game_start", "<@" + inGame.get() + ">");
	}

	@Override
	protected void runtime(String value) throws InvocationTargetException, IllegalAccessException {
		Pair<Method, JSONObject> action = toAction(StringUtils.stripAccents(value).toLowerCase(Locale.ROOT));
		if (action != null) {
			action.getFirst().invoke(this, action.getSecond());
		}
	}

	@PlayerAction("(?<word>[a-zA-Z]{3,})")
	private void nextWord(JSONObject args) {
		String word = args.getString("word");

		try {
			if (scanWord(word)) {
				if (!used.contains(word)) {
					getChannel().sendMessage(locale.get("str/game_out_shiritori", "<@" + inGame.get() + ">")).queue();
					inGame.remove(inGame.get());

					if (inGame.size() == 1) {
						reportResult(GameReport.SUCCESS, "str/game_end_alt", inGame.get());
						return;
					}

					nextTurn();
					return;
				}

				if (current != null && !current.substring(current.length() - 2).equals(word.substring(0, 2))) {
					getChannel().sendMessage(locale.get("error/invalid_word")).queue();
					return;
				}

				used.add(current);
				nextTurn();
			}
		} catch (FileNotFoundException e) {
			getChannel().sendMessage(locale.get("error/dict_not_found")).queue();
			close(GameReport.DICT_NOT_FOUND);
		}
	}

	public I18N getLocale() {
		return locale;
	}

	public String[] getPlayers() {
		return players;
	}

	private boolean scanWord(String word) throws FileNotFoundException {
		try (Scanner s = new Scanner(new FileInputStream(dict))) {
			char c = word.charAt(0);

			int idx = 0;
			int padSize = 0;
			int lookupIndex = 0;
			int charIndex = c - 'a';

			while (s.hasNextLine()) {
				String line = s.nextLine();

				if (idx == 0) {
					padSize = Integer.parseInt(line);
				} else if (idx == 1) {
					int range = padSize + 1;
					lookupIndex = Integer.parseInt(line.substring(range * charIndex + 1, range * (charIndex + 1)));
				} else if (idx > lookupIndex) {
					if (line.charAt(0) != c) {
						return false;
					} else if (line.equals(word)) {
						return true;
					}
				}

				idx++;
			}
		}

		return false;
	}

	private void reportEvent(String msg, Object... args) {
		getChannel().sendMessage(locale.get(msg, args))
				.queue(m -> {
					if (message != null) {
						TextChannel channel = Main.getApp().getShiro().getTextChannelById(message.getFirst());
						if (channel != null) {
							channel.retrieveMessageById(message.getSecond())
									.flatMap(Objects::nonNull, Message::delete)
									.queue();
						}
					}

					message = new Pair<>(m.getTextChannel().getId(), m.getId());
				});
	}

	private void reportResult(@MagicConstant(valuesFromClass = GameReport.class) byte code, String msg, Object... args) {
		close(code);
		getChannel().sendMessage(locale.get(msg, args))
				.queue(m -> {
					if (message != null) {
						TextChannel channel = Main.getApp().getShiro().getTextChannelById(message.getFirst());
						if (channel != null) {
							channel.retrieveMessageById(message.getSecond())
									.flatMap(Objects::nonNull, Message::delete)
									.queue();
						}
					}
				});
	}

	@Override
	protected void nextTurn() {
		super.nextTurn();

		if (getTurn() >= 50) {
			if (getTurn() == 50) {
				getChannel().sendMessage(locale.get("alert/shiritori_sudden_death")).queue();
				setTimeout(turn -> {
					getChannel().sendMessage(locale.get("str/game_wo_alt", "<@" + inGame.get() + ">", "<@" + inGame.peekNext() + ">")).queue();
					inGame.remove(inGame.get());

					if (inGame.size() == 1) {
						reportResult(GameReport.SUCCESS, "str/game_end_alt", inGame.get());
						return;
					}

					nextTurn();
				}, 30, TimeUnit.SECONDS);
			}

			reportEvent("alert/game_turn_change_shiritori", "<@" + inGame.getNext() + ">", current);
		} else {
			reportEvent("str/game_turn_change_shiritori", "<@" + inGame.getNext() + ">", current);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Shiritori shiritori = (Shiritori) o;
		return seed == shiritori.seed;
	}

	@Override
	public int hashCode() {
		return Objects.hash(seed);
	}
}
