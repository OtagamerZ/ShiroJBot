/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.handlers.games.tabletop.games.shoukan.enums;

import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hand;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Shoukan;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.SlotColumn;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import org.apache.commons.lang3.tuple.Triple;

import java.util.List;
import java.util.function.BiConsumer;

public enum Effect {
	NONE((args, defs) -> {
	}),
	DRAW_CARD((args, defs) -> {
		defs.getMiddle().draw();
	}),
	DRAW_X_CARD((args, defs) -> {
		defs.getMiddle().draw(CardDAO.getCard(args[0], false));
	}),
	DRAW_N_CARDS((args, defs) -> {
		for (int i = 0; i < Integer.parseInt(args[0]); i++) {
			defs.getMiddle().draw();
		}
	}),
	CONVERT_CARD((args, defs) -> {

	}),
	KILL_CARD((args, defs) -> {

	}),
	KILL_TYPE((args, defs) -> {

	}),
	KILL_EQUIPMENTS((args, defs) -> {

	}),
	REVIVE_CARD((args, defs) -> {
		Drawable d = defs.getLeft().getArena().getGraveyard().get(defs.getMiddle().getSide()).pollLast();
		List<SlotColumn<Drawable, Drawable>> slots = defs.getLeft().getArena().getSlots().get(defs.getMiddle().getSide());

		if (d instanceof Champion) {
			int slotAvailable = -1;
			for (int i = 0; i < slots.size(); i++) {
				if (slots.get(i).getTop() == null) {
					slotAvailable = i;
					break;
				}
			}

			if (slotAvailable == -1) {
				defs.getMiddle().getCards().add(d);
			} else {
				if (defs.getRight().getCard().getId().equalsIgnoreCase("ORIHIME_INOUE") && d.getCard().getId().equals("ICHIGO_KUROSAKI")) {
					d = CardDAO.getFusionByName("VASTO_LORDE_ICHIGO");
					assert d != null;
					slots.get(slotAvailable).setTop(d.copy());
					return;
				}
				slots.get(slotAvailable).setTop(d);
			}
		} else if (d != null) {
			defs.getMiddle().getCards().add(d);
		}
	}),
	SWITCH_TO_DEFENSE((args, defs) -> {
		((Champion) defs.getRight()).setDefending(true);
	});

	private final BiConsumer<String[], Triple<Shoukan, Hand, Drawable>> effect;

	Effect(BiConsumer<String[], Triple<Shoukan, Hand, Drawable>> effect) {
		this.effect = effect;
	}

	public BiConsumer<String[], Triple<Shoukan, Hand, Drawable>> getEffect() {
		return effect;
	}
}
