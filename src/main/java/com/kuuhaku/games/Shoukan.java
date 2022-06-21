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

package com.kuuhaku.games;

import com.kuuhaku.games.engine.GameInstance;
import com.kuuhaku.model.common.shoukan.Arena;
import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.utils.IO;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Predicate;

public class Shoukan extends GameInstance {
	private final I18N locale;
	private final String[] players;
	private final Map<Side, Hand> hands;
	private final Arena arena = new Arena(this);

	public Shoukan(I18N locale, User p1, User p2) {
		this(locale, p1.getId(), p2.getId());
	}

	public Shoukan(I18N locale, String p1, String p2) {
		this.locale = locale;
		this.players = new String[]{p1, p2};
		this.hands = Map.of(
				Side.TOP, new Hand(p1, Side.TOP),
				Side.BOTTOM, new Hand(p2, Side.BOTTOM)
		);
	}

	@Override
	protected boolean validate(Message message) {
		return ((Predicate<Message>) m -> ArrayUtils.contains(players, m.getAuthor().getId()))
				.and(m -> m.getContentRaw().matches("^reload|\\d+(,(\\d+|[a-z]))*$"))
				.and(m -> getTurn() % 2 == Arrays.binarySearch(players, m.getAuthor().getId()))
				.test(message);
	}

	@Override
	protected void begin() {
		getChannel().sendMessage(locale.get("str/game_start", "<@" + getCurrent().getUid() + ">"))
				.addFile(IO.getBytes(arena.render(locale), "webp"), "game.webp")
				.queue();
	}

	@Override
	protected void runtime(String value) {


		getChannel().sendMessage(locale.get("str/game_turn_change", "<@" + getCurrent().getUid() + ">", getTurn()))
				.addFile(IO.getBytes(arena.render(locale), "webp"), "game.webp")
				.queue();
	}

	public Map<Side, Hand> getHands() {
		return hands;
	}

	public Hand getCurrent() {
		return hands.get(getTurn() % 2 == 0 ? Side.TOP : Side.BOTTOM);
	}
}
