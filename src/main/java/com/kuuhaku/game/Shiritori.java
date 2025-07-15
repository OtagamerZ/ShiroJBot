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

package com.kuuhaku.game;

import com.kuuhaku.Main;
import com.kuuhaku.game.engine.GameInstance;
import com.kuuhaku.game.engine.GameReport;
import com.kuuhaku.game.engine.NullPhase;
import com.kuuhaku.game.engine.PlayerAction;
import com.kuuhaku.model.common.InfiniteList;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONObject;
import kotlin.Pair;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.apache.commons.lang3.StringUtils;
import org.intellij.lang.annotations.MagicConstant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Shiritori extends GameInstance<NullPhase> {
	private static final String[] blacklisted = {};

	private final long seed = ThreadLocalRandom.current().nextLong();
	private final InfiniteList<String> inGame;
	private final File dict;
	private final Set<String> used = new HashSet<>();
	private Pair<String, String> message;
	private String current;

	public Shiritori(I18N locale, User... players) {
		this(locale, Arrays.stream(players).map(User::getId).toArray(String[]::new));
	}

	public Shiritori(I18N locale, String... players) {
		super(locale, players);

		this.inGame = new InfiniteList<>(Set.of(players));
		this.dict = IO.getResourceAsFile("shiritori/" + locale.name().toLowerCase() + ".dict");

		setTimeout(turn -> {
			getChannel().sendMessage(locale.get("str/game_wo_alt", "<@" + inGame.get() + ">", "<@" + inGame.peekNext() + ">")).queue();
			inGame.remove();

			if (inGame.size() == 1) {
				reportResult(GameReport.SUCCESS, "str/game_end_alt", "<@" + inGame.get() + ">");
				return;
			}

			nextTurn();
		}, 1, TimeUnit.MINUTES);
	}

	@Override
	protected boolean validate(Message message) {
		return message.getAuthor().getId().equals(inGame.get());
	}

	@Override
	protected void begin() {
		if (this.dict == null) {
			getChannel().sendMessage(getString("error/dict_not_found")).queue();
			close(GameReport.DICT_NOT_FOUND);
			return;
		}

		reportEvent("str/game_start_shiritori", "<@" + inGame.get() + ">");
	}

	@Override
	protected void runtime(User user, String value) throws InvocationTargetException, IllegalAccessException {
		Pair<Method, JSONObject> action = toAction(StringUtils.stripAccents(value).toLowerCase());
		if (action != null) {
			action.getFirst().invoke(this, action.getSecond());
		}
	}

	@PlayerAction("(?<word>[a-zA-Z]{3,})")
	private void nextWord(JSONObject args) {
		String word = args.getString("word").toLowerCase();

		try {
			if (scanWord(word)) {
				if (used.contains(word)) {
					getChannel().sendMessage(getString("str/game_out_shiritori", "<@" + inGame.get() + ">")).queue();
					inGame.remove();

					if (inGame.size() == 1) {
						reportResult(GameReport.SUCCESS, "str/game_end_alt", "<@" + inGame.get() + ">");
						return;
					}

					nextTurn();
					return;
				}

				if (current != null) {
					String end = current.substring(current.length() - 2);
					if (Utils.equalsAny(end, blacklisted)) {
						getChannel().sendMessage(getString("error/blacklisted_ending")).queue();
						return;
					} else if (!end.equals(word.substring(0, 2))) {
						getChannel().sendMessage(getString("error/invalid_word")).queue();
						return;
					}
				}

				used.add(current = word);
				nextTurn();
			}
		} catch (FileNotFoundException e) {
			getChannel().sendMessage(getString("error/dict_not_found")).queue();
			close(GameReport.DICT_NOT_FOUND);
		}
	}

	private boolean scanWord(String word) throws FileNotFoundException {
		try (Scanner s = new Scanner(new FileInputStream(dict), StandardCharsets.UTF_8)) {
			char c = word.charAt(0);

			int idx = 0;
			int padSize = 0;
			int lookupIndex = 0;

			while (s.hasNextLine()) {
				String line = s.nextLine();

				if (idx == 0) {
					padSize = Integer.parseInt(line);
				} else if (idx == 1) {
					int charIndex = line.indexOf(c);
					if (charIndex == -1) return false;

					lookupIndex = Integer.parseInt(line.substring(charIndex + 1, charIndex + 1 + padSize));
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
		getChannel().sendMessage(getString(msg, args))
				.queue(m -> {
					if (message != null) {
						GuildMessageChannel channel = Main.getApp().getMessageChannelById(message.getFirst());
						if (channel != null) {
							channel.retrieveMessageById(message.getSecond())
									.flatMap(Objects::nonNull, Message::delete)
									.queue(null, Utils::doNothing);
							message = null;
						}
					}

					message = new Pair<>(m.getChannel().getId(), m.getId());
				}, Utils::doNothing);
	}

	private void reportResult(@MagicConstant(valuesFromClass = GameReport.class) byte code, String msg, Object... args) {
		try {
			if (isClosed()) return;

			getChannel().sendMessage(getString(msg, args))
					.queue(m -> {
						if (message != null) {
							GuildMessageChannel channel = Main.getApp().getMessageChannelById(message.getFirst());
							if (channel != null) {
								channel.retrieveMessageById(message.getSecond())
										.flatMap(Objects::nonNull, Message::delete)
										.queue(null, Utils::doNothing);
								message = null;
							}
						}
					}, Utils::doNothing);
		} finally {
			close(code);
		}
	}

	@Override
	public void nextTurn() {
		nextTurn(false);
	}

	protected void nextTurn(boolean same) {
		if (!same) {
			super.nextTurn();
		}

		if (getTurn() >= 50) {
			if (getTurn() == 50) {
				getChannel().sendMessage(getString("alert/shiritori_sudden_death")).queue();
				setTimeout(turn -> {
					getChannel().sendMessage(getString("str/game_wo_alt", "<@" + inGame.get() + ">", "<@" + inGame.peekNext() + ">")).queue();
					inGame.remove();

					if (inGame.size() == 1) {
						reportResult(GameReport.SUCCESS, "str/game_end_alt", "<@" + inGame.get() + ">");
						return;
					}

					nextTurn(true);
				}, 30, TimeUnit.SECONDS);
			}

			if (!same) {
				reportEvent("alert/game_turn_change_shiritori", "<@" + inGame.getNext() + ">", current);
			}
		} else if (!same) {
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
		return Objects.hashCode(seed);
	}
}
