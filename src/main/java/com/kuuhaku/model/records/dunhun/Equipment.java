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

import com.kuuhaku.Constants;
import com.kuuhaku.model.enums.dunhun.GearSlot;
import com.kuuhaku.model.persistent.dunhun.Gear;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONArray;
import com.ygimenez.json.JSONObject;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.kuuhaku.model.enums.dunhun.GearSlot.*;

public final class Equipment implements Iterable<Gear>, Serializable {
	private Gear helmet;
	private Gear body;
	private Gear boots;
	private Gear gloves;
	private Gear back;
	private Gear amulet;
	private Gear belt;
	private Pair<Gear, Gear> rings = new Pair<>(null, null);
	private Pair<Gear, Gear> weapons = new Pair<>(null, null);

	public Equipment() {
	}

	public Equipment(BiFunction<GearSlot, Integer, Gear> supplier) {
		helmet = supplier.apply(HELMET, -1);
		body = supplier.apply(BODY, -1);
		boots = supplier.apply(BOOTS, -1);
		gloves = supplier.apply(GLOVES, -1);
		back = supplier.apply(BACK, -1);
		amulet = supplier.apply(AMULET, -1);
		belt = supplier.apply(BELT, -1);
		rings = new Pair<>(supplier.apply(RING, 0), supplier.apply(RING, 1));
		weapons = new Pair<>(supplier.apply(WEAPON, 0), supplier.apply(WEAPON, 1));
	}

	public Gear getHelmet() {
		return helmet;
	}

	public Gear getBody() {
		return body;
	}

	public Gear getBoots() {
		return boots;
	}

	public Gear getGloves() {
		return gloves;
	}

	public Gear getBack() {
		return back;
	}

	public Gear getAmulet() {
		return amulet;
	}

	public Gear getBelt() {
		return belt;
	}

	public Pair<Gear, Gear> getRings() {
		return rings;
	}

	public Pair<Gear, Gear> getWeapons() {
		return weapons;
	}

	public boolean equip(Gear gear) {
		AtomicBoolean equipped = new AtomicBoolean();
		withSlot(gear.getBasetype().getStats().slot(), g -> {
			if (g == null) {
				equipped.set(true);
				return gear;
			}

			return g;
		});

		return equipped.get();
	}

	public void unequip(Gear gear) {
		withSlot(gear.getBasetype().getStats().slot(), g -> Objects.equals(g, gear) ? null : g);
	}

	public void withSlot(GearSlot slot, Function<Gear, Gear> action) {
		try {
			if (Utils.equalsAny(slot, RING, WEAPON)) {
				if (slot == RING) {
					rings = new Pair<>(
							action.apply(rings.getFirst()),
							action.apply(rings.getSecond())
					);
				} else {
					weapons = new Pair<>(
							action.apply(weapons.getFirst()),
							action.apply(weapons.getSecond())
					);
				}
				return;
			}

			Field f = getClass().getDeclaredField(slot.name().toLowerCase());
			f.set(this, action.apply((Gear) f.get(this)));
		} catch (NoSuchFieldException | IllegalAccessException e) {
			Constants.LOGGER.error(e, e);
		}
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
				Map.entry(BELT.name(), belt),
				Map.entry(RING.name(), JSONArray.of(rings.getFirst(), rings.getSecond())),
				Map.entry(WEAPON.name(), JSONArray.of(weapons.getFirst(), weapons.getSecond()))
		).toString();
	}

	@NotNull
	@Override
	public Iterator<Gear> iterator() {
		Gear[] gears = {
				helmet, body, boots, gloves, back, amulet, belt,
				rings.getFirst(), rings.getSecond(),
				weapons.getFirst(), weapons.getSecond()
		};

		return Arrays.stream(gears).iterator();
	}
}
