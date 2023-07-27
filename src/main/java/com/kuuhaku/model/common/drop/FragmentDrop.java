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

package com.kuuhaku.model.common.drop;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.Rarity;
import com.kuuhaku.model.persistent.user.UserItem;
import com.kuuhaku.util.Calc;

public class FragmentDrop extends Drop {
	public FragmentDrop(I18N locale, Rarity rarity) {
		this(locale, rarity,  6 - rarity.getIndex() + Calc.rng(0, 10 / rarity.getIndex()));
	}

	private FragmentDrop(I18N locale, Rarity rarity, int value) {
		super(rarity,
				r -> {
					UserItem i = DAO.find(UserItem.class, Rarity.values()[r - 1] + "_SHARD");
					return locale.get("str/drop_content", Math.min(value, 18 - r * 3) + "x " + i.getInfo(locale));
				},
				(r, acc) -> {
					UserItem i = DAO.find(UserItem.class, Rarity.values()[r - 1] + "_SHARD");
					acc.addItem(i, Math.min(value, 18 - r * 3));
				}
		);
	}
}
