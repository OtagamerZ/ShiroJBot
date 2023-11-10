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

package com.kuuhaku.model.common.shoukan;

import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Senshi;

import java.util.Objects;

public class FlagSource {
	private final Drawable<?> source;

	private final Side side;
	private final int hash;

	public FlagSource(Drawable<?> source) {
		this.source = source;
		this.side = source == null ? null : source.getSide();

		if (source instanceof Evogear e) {
			this.hash = e.posHash();
		} else if (source instanceof Senshi s) {
			this.hash = s.posHash();
		} else if (source != null && source.getHand() != null) {
			this.hash = source.getSlot().hashCode();
		} else {
			this.hash = -1;
		}
	}

	public boolean isExpired() {
		if (side != null) {
			if (source instanceof Evogear e && !e.isSpell() && (e.getEquipper() == null || e.posHash() != hash)) {
				return true;
			}

			return source instanceof Senshi s && s.posHash() != hash;
		}

		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FlagSource that = (FlagSource) o;
		return hash == that.hash && Objects.equals(source, that.source) && side == that.side;
	}

	@Override
	public int hashCode() {
		return Objects.hash(source, side, hash);
	}
}
