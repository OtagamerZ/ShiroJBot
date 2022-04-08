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
import com.kuuhaku.handlers.games.tabletop.games.shoukan.AppliedDebuff;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Stash;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.TriFunction;
import org.apache.commons.lang3.StringUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public enum Reward {
	XP("XP", (h, v, apply) -> {
		if (!apply) return Helper.separate(v) + " XP";
		int r = Math.abs(v);

		v = (int) (v * (1 + h.getLevel() * 0.01));
		
		if (v >= 0) h.addXp(r);
		else h.removeXp(r);
		KawaiponDAO.saveHero(h);

		return Helper.separate(v < 0 ? -r : r) + " XP";
	}),
	EP("EP", (h, v, apply) -> {
		if (!apply) return Helper.separate(v) + " EP";
		int r = Math.abs(v);

		if (v >= 0) h.rest(r);
		else h.removeEnergy(r);
		KawaiponDAO.saveHero(h);

		return Helper.separate(v < 0 ? -r : r) + " EP";
	}),
	CREDIT("CR", (h, v, apply) -> {
		if (!apply) return Helper.separate(v) + " CR";
		int r = Math.abs(v);

		Account acc = AccountDAO.getAccount(h.getUid());
		if (v >= 0) acc.addCredit(r, Reward.class);
		else acc.removeCredit(r, Reward.class);
		AccountDAO.saveAccount(acc);

		return Helper.separate(v < 0 ? -r : r) + " CR";
	}),
	GEM("Gemas", (h, v, apply) -> {
		if (!apply) return Helper.separate(v) + " gema" + (Math.abs(v) == 1 ? "" : "s");
		int r = Math.abs(v);

		Account acc = AccountDAO.getAccount(h.getUid());
		if (v >= 0) acc.addGem(r);
		else acc.removeGem(r);
		AccountDAO.saveAccount(acc);

		return Helper.separate(v < 0 ? -r : r) + " gema" + (Math.abs(v) == 1 ? "" : "s");
	}),
	EQUIPMENT("Equipamento", (h, v, apply) -> {
		if (!apply) return Helper.clamp(v, 0, 100) + "% de chance";
		String r = "Nenhum";

		if (Helper.chance(Helper.clamp(v, 0, 100))) {
			Equipment e = CardDAO.getRandomEquipment(false);
			assert e != null;
			StashDAO.saveCard(new Stash(h.getUid(), e));

			r = e.getCard().getName() + " (" + StringUtils.repeat("\uD83D\uDFCA", e.getTier()) + ")";
		}

		return r;
	}),
	SPELL("Magia", (h, v, apply) -> {
		if (!apply) return Helper.clamp(v, 0, 100) + "% de chance";
		String r = "Nenhum";

		if (Helper.chance(Helper.clamp(v, 0, 100))) {
			Equipment e = CardDAO.getRandomEquipment(true);
			assert e != null;
			StashDAO.saveCard(new Stash(h.getUid(), e));

			r = e.getCard().getName() + " (" + StringUtils.repeat("\uD83D\uDFCA", e.getTier()) + ")";
		}

		return r;
	}),
	CLEANSE("Purificação", (h, v, apply) -> {
		if (!apply) return Helper.clamp(v, 0, 100) + "% de chance";
		String r = "Falhou";

		if (Helper.chance(Helper.clamp(v, 0, 100))) {
			for (AppliedDebuff d : h.getDebuffs()) {
				d.setExpiration(ZonedDateTime.now(ZoneId.of("GMT-3")));
			}
			r = "Sucesso";
		}

		return r;
	}),
	;

	private final String name;
	private final TriFunction<Hero, Integer, Boolean, String> evt;

	Reward(String name, TriFunction<Hero, Integer, Boolean, String> evt) {
		this.name = name;
		this.evt = evt;
	}

	public String apply(Hero h, int value, boolean apply) {
		return evt.apply(h, value, apply);
	}

	@Override
	public String toString() {
		return name;
	}
}
