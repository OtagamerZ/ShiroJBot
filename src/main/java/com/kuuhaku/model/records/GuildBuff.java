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

package com.kuuhaku.model.records;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public record GuildBuff(String id, long expiration, double card, double drop, double rarity, double xp) {
	public GuildBuff(String id, int time, TimeUnit unit, double card, double drop, double rarity, double xp) {
		this(id, System.currentTimeMillis() + unit.toMillis(time), card, drop, rarity, xp);
	}

	public GuildBuff(double card, double drop, double rarity, double xp) {
		this("", 0, card, drop, rarity, xp);
	}

	public boolean expired() {
		return expiration < System.currentTimeMillis();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		GuildBuff guildBuff = (GuildBuff) o;
		return Objects.equals(id, guildBuff.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
