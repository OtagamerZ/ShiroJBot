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

import com.kuuhaku.game.Shoukan;
import com.kuuhaku.util.IO;
import com.ygimenez.json.JSONUtils;

import java.io.IOException;
import java.util.stream.Collectors;

public record Global(byte[] field, byte[] banned) {
	public Global(Shoukan game) throws IOException {
		this(
				JSONUtils.toJSON(game.getArena().getField()),
				game.getArena().getBanned().stream()
						.map(JSONUtils::toJSON)
						.collect(Collectors.joining(",", "[", "]"))
		);
	}

	public Global(String field, String banned) throws IOException {
		this(IO.compress(field), IO.compress(banned));
	}
}
