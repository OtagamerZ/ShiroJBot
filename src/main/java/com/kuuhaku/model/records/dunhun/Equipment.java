/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2024  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.records.dunhun;

import com.kuuhaku.model.enums.dunhun.GearSlot;
import com.kuuhaku.model.persistent.dunhun.Gear;
import com.ygimenez.json.JSONArray;
import com.ygimenez.json.JSONObject;
import kotlin.Pair;

import java.util.Map;
import java.util.function.BiFunction;

import static com.kuuhaku.model.enums.dunhun.GearSlot.*;

public record Equipment(
	Gear helmet,
	Gear body,
	Gear boots,
	Gear gloves,
	Gear back,
	Gear amulet,
	Pair<Gear, Gear> rings,
	Pair<Gear, Gear> weapons
) {
	public static Equipment empty() {
		return new Equipment();
	}

	public static Equipment fromSupplier(BiFunction<GearSlot, Integer, Gear> supplier) {
		return new Equipment(
				supplier.apply(HELMET, -1),
				supplier.apply(BODY, -1),
				supplier.apply(BOOTS, -1),
				supplier.apply(GLOVES, -1),
				supplier.apply(BACK, -1),
				supplier.apply(AMULET, -1),
				new Pair<>(
						supplier.apply(RING, 0),
						supplier.apply(RING, 1)
				),
				new Pair<>(
						supplier.apply(WEAPON, 0),
						supplier.apply(WEAPON, 1)
				)
		);
	}

	@Override
	public String toString() {
		return JSONObject.of(
				Map.entry(HELMET.name(), helmet),
				Map.entry(BODY.name(), body),
				Map.entry(BOOTS.name(), boots),
				Map.entry(GLOVES.name(), gloves),
				Map.entry(BACK.name(), back),
				Map.entry(AMULET.name(), amulet),
				Map.entry(RING.name(), JSONArray.of(rings.getFirst(), rings.getSecond())),
				Map.entry(WEAPON.name(), JSONArray.of(weapons.getFirst(), weapons.getSecond()))
		).toString();
	}
}
