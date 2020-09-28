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
import com.kuuhaku.handlers.games.tabletop.games.shoukan.*;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import org.apache.commons.lang3.tuple.Pair;
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
		List<SlotColumn<Drawable, Drawable>> slots = defs.getLeft()
				.getArena()
				.getSlots()
				.get(defs.getMiddle().getSide() == Side.TOP ? Side.BOTTOM : Side.TOP);

		for (int i = 0; i < slots.size(); i++) {
			Champion c = (Champion) slots.get(i).getTop();
			if (c != null && c.getRace() == Race.valueOf(args[0]) && !c.equals(defs.getRight()))
				defs.getLeft().killCard(
						defs.getMiddle().getSide() == Side.TOP ? Side.BOTTOM : Side.TOP,
						i,
						slots,
						defs.getLeft().getHands().get(defs.getMiddle().getSide() == Side.TOP ? Side.BOTTOM : Side.TOP)
				);
		}
	}),
	PURGE_TYPE((args, defs) -> {
		defs.getLeft()
				.getArena()
				.getSlots()
				.forEach((k, s) -> {
					for (int i = 0; i < s.size(); i++) {
						Champion c = (Champion) s.get(i).getTop();
						if (c != null && c.getRace() == Race.valueOf(args[0]) && !c.equals(defs.getRight()))
							defs.getLeft().killCard(
									k,
									i,
									s,
									defs.getLeft().getHands().get(k)
							);
					}
				});
	}),
	KILL_EQUIPMENTS((args, defs) -> {
		defs.getLeft()
				.getArena()
				.getSlots()
				.get(defs.getMiddle().getSide() == Side.TOP ? Side.BOTTOM : Side.TOP)
				.forEach(s -> {
					((Equipment) s.getBottom()).setLinkedTo(null);

					defs.getLeft().getArena().getGraveyard().get(defs.getMiddle().getSide() == Side.TOP ? Side.BOTTOM : Side.TOP).add(s.getBottom());
					s.setBottom(null);
				});
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


				slots.get(slotAvailable).setTop(d);
			}
		} else if (d != null) {
			defs.getMiddle().getCards().add(d);
		}
	}),
	SWITCH_TO_DEFENSE((args, defs) -> {
		defs.getRight().getLeft().setDefending(true);
	}),
	INCREASE_MANA((args, defs) -> {
		defs.getMiddle().addMana(Integer.parseInt(args[0]));
	}),
	INCREASE_HP((args, defs) -> {
		defs.getMiddle().addHp(Integer.parseInt(args[0]));
	}),
	DECREASE_MANA((args, defs) -> {
		defs.getMiddle().removeMana(Integer.parseInt(args[0]));
	}),
	DECREASE_HP((args, defs) -> {
		defs.getMiddle().removeHp(Integer.parseInt(args[0]));
	}),
	LEECH_MANA((args, defs) -> {
		defs.getLeft().getHands().get(defs.getMiddle().getSide() == Side.TOP ? Side.BOTTOM : Side.TOP).removeMana(Integer.parseInt(args[0]));
	}),
	LEECH_HP((args, defs) -> {
		defs.getLeft().getHands().get(defs.getMiddle().getSide() == Side.TOP ? Side.BOTTOM : Side.TOP).removeHp(Integer.parseInt(args[0]));
	}),
	STRONG_VS_TYPE((args, defs) -> {
	}),
	WEAK_VS_TYPE((args, defs) -> {
	}),
	PARALYZE_CARD((args, defs) -> {

	});

	private final BiConsumer<String[], Triple<Shoukan, Hand, Pair<Champion, Champion>>> effect;

	Effect(BiConsumer<String[], Triple<Shoukan, Hand, Pair<Champion, Champion>>> effect) {
		this.effect = effect;
	}

	public BiConsumer<String[], Triple<Shoukan, Hand, Pair<Champion, Champion>>> getEffect() {
		return effect;
	}
}
