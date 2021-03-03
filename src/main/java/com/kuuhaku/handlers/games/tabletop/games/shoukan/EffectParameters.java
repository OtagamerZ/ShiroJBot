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

package com.kuuhaku.handlers.games.tabletop.games.shoukan;

import com.kuuhaku.handlers.games.tabletop.framework.GameChannel;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.EffectTrigger;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EffectParameters {
	private final EffectTrigger trigger;
	private final Shoukan shoukan;
	private final int index;
	private final Side side;
	private final Map<Side, Hand> hands;
	private final Map<Side, List<SlotColumn<Champion, Equipment>>> slots;
	private final Map<Side, LinkedList<Drawable>> graveyard;
	private final Duelists duelists;
	private final GameChannel channel;

	public EffectParameters(EffectTrigger trigger, Shoukan shoukan, int index, Side side, Duelists duelists, GameChannel channel) {
		this.trigger = trigger;
		this.shoukan = shoukan;
		this.index = index;
		this.side = side;
		this.hands = shoukan.getHands();
		this.slots = shoukan.getArena().getSlots();
		this.graveyard = shoukan.getArena().getGraveyard();
		this.duelists = duelists;
		this.channel = channel;
	}

	public EffectParameters() {
		this.trigger = EffectTrigger.NONE;
		this.shoukan = null;
		this.index = -1;
		this.side = null;
		this.hands = new HashMap<>();
		this.slots = new HashMap<>();
		this.graveyard = new HashMap<>();
		this.duelists = null;
		this.channel = null;
	}

	public EffectTrigger getTrigger() {
		return trigger;
	}

	public Shoukan getShoukan() {
		return shoukan;
	}

	public int getIndex() {
		return index;
	}

	public Side getSide() {
		return side;
	}

	public Map<Side, Hand> getHands() {
		return hands;
	}

	public Map<Side, List<SlotColumn<Champion, Equipment>>> getSlots() {
		return slots;
	}

	public Map<Side, LinkedList<Drawable>> getGraveyard() {
		return graveyard;
	}

	public Champion getChampionFromGrave(Side s) {
		return shoukan.getChampionFromGrave(s);
	}

	public Equipment getEquipmentFromGrave(Side s) {
		return shoukan.getEquipmentFromGrave(s);
	}

	public Duelists getDuelists() {
		return duelists;
	}

	public Map<Race, Integer> countTypesInGraveyard(Side side) {
		Map<Race, Integer> types = new HashMap<>();

		for (Race race : Race.values()) {
			types.put(race, (int) graveyard.get(side).stream()
					.filter(d -> d instanceof Champion)
					.map(d -> (Champion) d)
					.filter(c -> c.getRace() == race)
					.count());
		}

		return types;
	}

	public GameChannel getChannel() {
		return channel;
	}
}
