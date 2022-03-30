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

import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.postgresql.StashDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.AppliedDebuff;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Evogear;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Stash;
import com.kuuhaku.utils.helpers.MathHelper;
import com.kuuhaku.utils.helpers.StringHelper;
import org.apache.commons.lang3.StringUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.BiFunction;

public enum Reward {
	XP("XP", (h, v) -> {
		if (h == null) return StringHelper.separate(v) + " XP";
		int r = Math.abs(v);

		if (v >= 0) h.addXp(r);
		else h.removeXp(r);
		KawaiponDAO.saveHero(h);

		return StringHelper.separate(v < 0 ? -r : r) + " XP";
	}),
	EP("EP", (h, v) -> {
		if (h == null) return StringHelper.separate(v) + " EP";
		int r = Math.abs(v);

		if (v >= 0) h.rest(r);
		else h.removeEnergy(r);
		KawaiponDAO.saveHero(h);

		return StringHelper.separate(v < 0 ? -r : r) + " EP";
	}),
	CREDIT("CR", (h, v) -> {
		if (h == null) return StringHelper.separate(v) + " CR";
		int r = Math.abs(v);

		Account acc = Account.find(Account.class, h.getUid());
		if (v >= 0) acc.addCredit(r, Reward.class);
		else acc.removeCredit(r, Reward.class);
		acc.save();

		return StringHelper.separate(v < 0 ? -r : r) + " CR";
	}),
	GEM("Gemas", (h, v) -> {
		if (h == null) return StringHelper.separate(v) + " gema" + (Math.abs(v) == 1 ? "" : "s");
		int r = Math.abs(v);

		Account acc = Account.find(Account.class, h.getUid());
		if (v >= 0) acc.addGem(r);
		else acc.removeGem(r);
		acc.save();

		return StringHelper.separate(v < 0 ? -r : r) + " gema" + (Math.abs(v) == 1 ? "" : "s");
	}),
	EQUIPMENT("Equipamento", (h, v) -> {
		if (h == null) return MathHelper.clamp(v, 0, 100) + "% de chance";
		String r = "Nenhum";

		if (MathHelper.chance(MathHelper.clamp(v, 0, 100))) {
			Evogear e = Evogear.queryNative(Evogear.class, """
					SELECT e
					FROM Evogear e
					WHERE (e.charms NOT LIKE '%SPELL%' AND e.charms NOT LIKE '%CURSE%')
					ORDER BY RANDOM()
					LIMIT 1
					""");
			assert e != null;
			StashDAO.saveCard(new Stash(h.getUid(), e));

			r = e.getCard().getName() + " (" + StringUtils.repeat("\uD83D\uDFCA", e.getTier()) + ")";
		}

		return r;
	}),
	SPELL("Magia", (h, v) -> {
		if (h == null) return MathHelper.clamp(v, 0, 100) + "% de chance";
		String r = "Nenhum";

		if (MathHelper.chance(MathHelper.clamp(v, 0, 100))) {
			Evogear e = Evogear.queryNative(Evogear.class, """
					SELECT e
					FROM Evogear e
					WHERE (e.charms LIKE '%SPELL%' OR e.charms LIKE '%CURSE%')
					ORDER BY RANDOM()
					LIMIT 1
					""");
			assert e != null;
			StashDAO.saveCard(new Stash(h.getUid(), e));

			r = e.getCard().getName() + " (" + StringUtils.repeat("\uD83D\uDFCA", e.getTier()) + ")";
		}

		return r;
	}),
	CLEANSE("Purificação", (h, v) -> {
		if (h == null) return MathHelper.clamp(v, 0, 100) + "% de chance";
		String r = "Falhou";

		if (MathHelper.chance(MathHelper.clamp(v, 0, 100))) {
			for (AppliedDebuff d : h.getDebuffs()) {
				d.setExpiration(ZonedDateTime.now(ZoneId.of("GMT-3")));
			}
			r = "Sucesso";
		}

		return r;
	}),
	;

	private final String name;
	private final BiFunction<Hero, Integer, String> evt;

	Reward(String name, BiFunction<Hero, Integer, String> evt) {
		this.name = name;
		this.evt = evt;
	}

	public String apply(Hero h, int value) {
		return evt.apply(h, value);
	}

	@Override
	public String toString() {
		return name;
	}
}
