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

package com.kuuhaku.model.records.shoukan;

import com.kuuhaku.util.Utils;

import java.util.List;

public record ShoukanParams(Integer hp, Integer mp, List<String> cards, Origin origin) {
	public ShoukanParams() {
		this(5000, 5, List.of(), null);
	}

	@Override
	public Integer hp() {
		return Utils.getOr(hp, 5000);
	}

	@Override
	public Integer mp() {
		return Utils.getOr(mp, 5);
	}

	@Override
	public List<String> cards() {
		return Utils.getOr(cards, List.of());
	}
}