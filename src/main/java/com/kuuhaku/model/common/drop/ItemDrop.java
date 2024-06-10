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
import com.kuuhaku.util.Utils;

import java.util.List;

public class ItemDrop extends Drop {
	public ItemDrop(I18N locale, Rarity rarity) {
		this(locale, rarity,  Calc.rng(Integer.MAX_VALUE));
	}

	private ItemDrop(I18N locale, Rarity rarity, int value) {
		super(rarity,
				r -> {
					List<UserItem> selection = switch (r) {
						case 1, 2 -> DAO.queryAll(UserItem.class, "SELECT i FROM UserItem i WHERE i.effect IS NOT NULL AND i.currency = 'CR'");
						case 3, 4 -> DAO.queryAll(UserItem.class, "SELECT i FROM UserItem i WHERE i.currency = 'CR'");
						default -> DAO.findAll(UserItem.class);
					};

					UserItem i = Utils.getRandomEntry(selection);
					return locale.get("str/drop_content", Math.min(value, 18 - r * 3) + "x " + i.getName(locale));
				},
				(r, acc) -> {
					List<UserItem> selection = switch (r) {
						case 1, 2 -> DAO.queryAll(UserItem.class, "SELECT i FROM UserItem i WHERE i.effect IS NOT NULL AND i.currency = 'CR'");
						case 3, 4 -> DAO.queryAll(UserItem.class, "SELECT i FROM UserItem i WHERE i.currency = 'CR'");
						default -> DAO.findAll(UserItem.class);
					};

					UserItem i = Utils.getRandomEntry(selection);
					acc.addItem(i, Math.min(value, 18 - r * 3));
				}
		);
	}
}
