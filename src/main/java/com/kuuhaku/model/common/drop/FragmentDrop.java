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
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.persistent.user.UserItem;
import com.kuuhaku.util.Calc;

public class FragmentDrop extends Drop {
	private final UserItem fragment;
	private final int amount;

	public FragmentDrop(Rarity rarity) {
		super(rarity);
		fragment = DAO.find(UserItem.class, Rarity.values()[rarity.getIndex() - 1] + "_SHARD");
		amount = 6 - Math.min(rarity.getIndex() + Calc.rng(0, 10 / rarity.getIndex()), 18 - rarity.getIndex() * 3);
	}

	@Override
	public String getContent(I18N locale) {
		return locale.get("str/drop_content", amount + "x " + fragment.getName(locale));
	}

	@Override
	public void apply(Account acc) {
		acc.addItem(fragment, amount);
	}
}
