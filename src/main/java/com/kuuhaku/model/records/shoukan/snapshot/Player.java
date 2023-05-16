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

package com.kuuhaku.model.records.shoukan.snapshot;

import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.json.JSONUtils;

import java.io.IOException;
import java.util.stream.Collectors;

public record Player(byte[] cards, byte[] deck, byte[] graveyard) {
	public Player(Hand h) throws IOException {
		this(
				h.getCards().stream().collect(Collectors.collectingAndThen(
						Collectors.toList(),
						JSONUtils::toJSON
				)),
				h.getDeck().stream().collect(Collectors.collectingAndThen(
						Collectors.toList(),
						JSONUtils::toJSON
				)),
				h.getGraveyard().stream().collect(Collectors.collectingAndThen(
						Collectors.toList(),
						JSONUtils::toJSON
				))
		);
	}

	public Player(String cards, String deck, String graveyard) throws IOException {
		this(IO.compress(cards), IO.compress(deck), IO.compress(graveyard));
	}
}
