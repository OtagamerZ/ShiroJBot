/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.postgresql.StashDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Stash;
import com.kuuhaku.utils.Helper;
import org.apache.commons.lang3.StringUtils;

import java.util.function.BiFunction;

public enum Reward {
	XP("XP", (h, v) -> {
		int r = Math.abs(v);

		if (v >= 0) h.addXp(r);
		else h.removeXp(r);
		KawaiponDAO.saveHero(h);

		return v < 0 ? -r : r;
	}),
	EP("EP", (h, v) -> {
		int r = Math.abs(v);

		if (v >= 0) h.rest(r);
		else h.removeEnergy(r);
		KawaiponDAO.saveHero(h);

		return v < 0 ? -r : r;
	}),
	CREDIT("CR", (h, v) -> {
		int r = Math.abs(v);

		Account acc = AccountDAO.getAccount(h.getUid());
		if (v >= 0) acc.addCredit(r, Reward.class);
		else acc.removeCredit(r, Reward.class);
		AccountDAO.saveAccount(acc);

		return v < 0 ? -r : r;
	}),
	GEM("Gemas", (h, v) -> {
		int r = Math.abs(v);

		Account acc = AccountDAO.getAccount(h.getUid());
		if (v >= 0) acc.addGem(r);
		else acc.removeGem(r);
		AccountDAO.saveAccount(acc);

		return v < 0 ? -r : r;
	}),
	EQUIPMENT("Equipamento", (h, v) -> {
		String r = "Nenhum";

		if (Helper.chance(v)) {
			Equipment e = CardDAO.getRandomEquipment(false);
			assert e != null;
			StashDAO.saveCard(new Stash(h.getUid(), e));

			r = e.getCard().getName() + " (" + StringUtils.repeat("\uD83D\uDFCA", e.getTier()) + ")";
		}

		return r;
	}),
	SPELL("Magia", (h, v) -> {
		String r = "Nenhum";

		if (Helper.chance(v)) {
			Equipment e = CardDAO.getRandomEquipment(true);
			assert e != null;
			StashDAO.saveCard(new Stash(h.getUid(), e));

			r = e.getCard().getName() + " (" + StringUtils.repeat("\uD83D\uDFCA", e.getTier()) + ")";
		}

		return r;
	});

	private final String name;
	private final BiFunction<Hero, Integer, Object> evt;

	Reward(String name, BiFunction<Hero, Integer, Object> evt) {
		this.name = name;
		this.evt = evt;
	}

	public Object apply(Hero h, int value) {
		return evt.apply(h, value);
	}

	@Override
	public String toString() {
		return name;
	}
}
