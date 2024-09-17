/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2024  Yago Gimenez (KuuHaKu)
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

import java.util.Objects;

public record SelectionRange(Integer min, Integer max) {
	public static final SelectionRange SINGLE = new SelectionRange(1);

	public SelectionRange(Integer min, Integer max) {
		this.min = Utils.getOr(min, 0);
		this.max = Utils.getOr(max, Integer.MAX_VALUE);
	}

	public SelectionRange(int amount) {
		this(amount, amount);
	}

	public boolean isRange() {
		return !Objects.equals(min, max);
	}

	public String label() {
		if (isRange()) {
			if (min == 0 && max == Integer.MAX_VALUE) return "str/select_any_cards";
			else if (min == 0) return "str/select_max_cards";
			else if (max == Integer.MAX_VALUE) return "str/select_min_cards";

			return "str/select_range_cards";
		}

		if (min > 1) return "str/select_many_cards";
		return "str/select_a_card";
	}
}
