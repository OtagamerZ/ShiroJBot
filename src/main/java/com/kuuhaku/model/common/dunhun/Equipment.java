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

package com.kuuhaku.model.common.dunhun;

import com.kuuhaku.Constants;
import com.kuuhaku.model.enums.dunhun.GearSlot;
import com.kuuhaku.model.persistent.dunhun.Gear;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONArray;
import com.ygimenez.json.JSONObject;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.kuuhaku.model.enums.dunhun.GearSlot.*;

public class Equipment implements Iterable<Gear> {
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

	public List<Gear> getRingList() {
		List<Gear> gears = new ArrayList<>();
		if (rings.getFirst() != null) gears.add(rings.getFirst());
		if (rings.getSecond() != null) gears.add(rings.getSecond());

		return gears;
	}

	public Pair<Gear, Gear> getWeapons() {
		return weapons;
	}

	public List<Gear> getWeaponList() {
		List<Gear> gears = new ArrayList<>();
		if (weapons.getFirst() != null) gears.add(weapons.getFirst());
		if (weapons.getSecond() != null) gears.add(weapons.getSecond());

		return gears;
	}

	public boolean equip(Gear gear) {
		unequip(gear);

		AtomicBoolean done = new AtomicBoolean();
		withSlot(gear.getBasetype().getStats().slot(), g -> {
			if (!done.get() && g == null) {
				done.set(true);
				return gear;
			}

			return g;
		});

		return done.get();
	}

	public boolean unequip(Gear gear) {
		AtomicBoolean done = new AtomicBoolean();
		withSlot(gear.getBasetype().getStats().slot(), g -> {
			if (!done.get() && Objects.equals(g, gear)) {
				done.set(true);
				return null;
			}

			return g;
		});

		return done.get();
	}

	public void withSlot(GearSlot slot, Function<Gear, Gear> action) {
		try {
			if (Utils.equalsAny(slot, RING, WEAPON)) {
				Function<Pair<Gear, Gear>, Pair<Gear, Gear>> proc = p -> {
					Gear dual = null;
					if (p.getFirst() != null && p.getFirst().getTags().contains("2-SLOT")) {
						dual = p.getFirst();
					} else if (p.getSecond() != null && p.getSecond().getTags().contains("2-SLOT")) {
						dual = p.getSecond();
					}

					if (dual != null) {
						return new Pair<>(action.apply(dual), null);
					} else {
						return new Pair<>(
								action.apply(p.getFirst()),
								action.apply(p.getSecond())
						);
					}
				};

				if (slot == RING) {
					rings = proc.apply(rings);
				} else {
					weapons = proc.apply(weapons);
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
		JSONObject jo = new JSONObject();
		for (GearSlot slot : values()) {
			withSlot(slot, g -> {
				if (g != null) {
					if (Utils.equalsAny(slot, RING, WEAPON)) {
						((JSONArray) jo.computeIfAbsent(slot.name(), k -> new JSONArray())).add(g.getId());
					} else {
						jo.put(slot.name(), g.getId());
					}
				}

				return g;
			});
		}

		return jo.toString();
	}

	@NotNull
	@Override
	public Iterator<Gear> iterator() {
		Gear[] gears = {
				helmet, body, boots, gloves, back, amulet, belt,
				rings.getFirst(), rings.getSecond(),
				weapons.getFirst(), weapons.getSecond()
		};

		return Arrays.stream(gears)
				.filter(Objects::nonNull)
				.iterator();
	}
}
