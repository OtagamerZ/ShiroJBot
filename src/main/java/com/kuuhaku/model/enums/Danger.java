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
import com.kuuhaku.handlers.games.tabletop.games.shoukan.AppliedDebuff;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;

import java.time.temporal.ChronoUnit;
import java.util.function.Function;

public enum Danger {
	EP("Penalidade de EP", h -> {
		if (h.getEnergy() <= 1) return "-0 EP";
		h.removeEnergy(1);

		return "-1 EP";
	}),
	XP("Penalidade de XP", h -> {
		int max = h.getXp();
		int penalty = Helper.rng(max / 10, max / 8);
		h.setXp(h.getXp() - penalty);

		return "-" + penalty + " XP";
	}),
	DEATH("Penalidade de morte", h -> {
		h.setDied(true);
		Kawaipon kp = KawaiponDAO.getKawaipon(h.getUid());
		kp.getHeroes().remove(h);
		KawaiponDAO.saveKawaipon(kp);

		return "Seu herói morreu durante a missão";
	}),
	EQUIPMENT("Penalidade de equipamento", h -> {
		if (h.getInventory().isEmpty()) return "Nenhum";
		h.getInventory().remove(Helper.getRandomEntry(h.getInventory()));

		return "Seu herói perdeu um dos equipamentos durante a missão";
	}),
	AGGRAVATE("Agravamento", h -> {
		for (AppliedDebuff d : h.getDebuffs()) {
			d.setExpiration(d.getExpiration().plus(d.getDebuff().getDuration(), ChronoUnit.SECONDS));
		}

		return "Suas maldições tiveram a duração aumentada";
	}),
	;

	private final String name;
	private final Function<Hero, String> evt;

	Danger(String name, Function<Hero, String> evt) {
		this.name = name;
		this.evt = evt;
	}

	public String apply(Hero h) {
		return evt.apply(h);
	}

	@Override
	public String toString() {
		return name;
	}
}
