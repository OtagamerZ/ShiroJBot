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

package com.kuuhaku.model.enums;

import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.persistent.shoukan.Evogear;

import java.util.Set;

public enum DeckOrder {
	NAME("ORDER BY d.card_id"),
	ATK("ORDER BY d.atk DESC, d.card_id"),
	DEF("ORDER BY d.dfs DESC, d.card_id"),
	ATTR("ORDER BY d.atk + d.dfs DESC, d.card_id"),
	COST("ORDER BY d.sacrifices DESC, d.blood DESC, d.mana DESC, d.card_id"),
	TIER("ORDER BY d.tier DESC", Evogear.class);

	private final String clause;
	private final Set<Class<? extends Drawable<?>>> allowed;

	@SafeVarargs
	DeckOrder(String clause, Class<? extends Drawable<?>>... allowed) {
		this.clause = clause;
		this.allowed = Set.of(allowed);
	}

	public String getClause() {
		return clause;
	}

	public boolean isAllowed(Class<? extends Drawable<?>> klass) {
		return allowed.isEmpty() || allowed.contains(klass);
	}

	@Override
	public String toString() {
		return clause;
	}
}
