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
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.kuuhaku.model.enums.dunhun.GearSlot.*;

public class Equipment implements Iterable<Gear> {
	private Gear helmet;
	private Gear body;
	private Gear boots;
	private Gear gloves;
	private Gear grimoire;
	private Gear amulet;
	private Gear belt;
	private final MultiSlot<Gear> rings = new MultiSlot<>(2);
	private final MultiSlot<Gear> weapons = new MultiSlot<>(2);

	public Equipment() {
	}

	public Equipment(BiFunction<GearSlot, Integer, Gear> supplier) {
		helmet = supplier.apply(HELMET, -1);
		body = supplier.apply(BODY, -1);
		boots = supplier.apply(BOOTS, -1);
		gloves = supplier.apply(GLOVES, -1);
		amulet = supplier.apply(AMULET, -1);
		belt = supplier.apply(BELT, -1);
		grimoire = supplier.apply(GRIMOIRE, -1);

		for (int i = 0; i < 2; i++) {
			rings.add(supplier.apply(RING, i));
			weapons.add(supplier.apply(WEAPON, i));
		}
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

	public Gear getGrimoire() {
		return grimoire;
	}

	public Gear getAmulet() {
		return amulet;
	}

	public Gear getBelt() {
		return belt;
	}

	public MultiSlot<Gear> getRings() {
		return rings;
	}

	public List<Gear> getRingList() {
		return List.copyOf(rings.getEntries());
	}

	public MultiSlot<Gear> getWeapons() {
		return weapons;
	}

	public List<Gear> getWeaponList() {
		return List.copyOf(weapons.getEntries());
	}

	public boolean equip(Gear gear) {
		unequip(gear);

		AtomicBoolean done = new AtomicBoolean();
		withSlot(gear.getBasetype().getStats().gearType().getSlot(), gear, g -> {
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
		withSlot(gear.getBasetype().getStats().gearType().getSlot(), null, g -> {
			if (!done.get() && Objects.equals(g, gear)) {
				done.set(true);
				return null;
			}

			return g;
		});

		return done.get();
	}

	public void withSlot(GearSlot slot, Gear ref, Function<Gear, Gear> action) {
		try {
			if (Utils.equalsAny(slot, RING, WEAPON)) {
				MultiSlot<Gear> items = slot == RING ? rings : weapons;
				if (ref != null) {
					if (ref.getTags().contains("2-SLOT") && !items.getEntries().isEmpty()) return;
				}

				for (int i = 0; i < items.getSize(); i++) {
					Gear curr = items.get(i);
					items.replace(curr, action.apply(curr));

					if (curr != null && curr.getTags().contains("2-SLOT")) break;
				}

				return;
			}

			Field f = getClass().getDeclaredField(slot.name().toLowerCase());
			f.set(this, action.apply((Gear) f.get(this)));
		} catch (NoSuchFieldException | IllegalAccessException e) {
			Constants.LOGGER.error(e, e);
		}
	}

	public JSONArray getWeaponTags() {
		JSONArray out = new JSONArray();

		int weapons = 0;
		boolean unarmed = true;
		for (Gear g : getWeapons().getEntries()) {
			if (g.isWeapon()) {
				if (!g.getTags().contains("UNARMED")) unarmed = false;
				weapons++;
			}

			out.addAll(g.getTags());
		}

		if (unarmed) {
			out.add("UNARMED");
			if (!out.contains("MELEE")) {
				out.add("MELEE");
			}
		}

		if (weapons >= 2) {
			out.add("DUAL_WIELD");
		}

		return out;
	}

	@Override
	public String toString() {
		JSONObject jo = new JSONObject();
		for (GearSlot slot : values()) {
			withSlot(slot, null, g -> {
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
				helmet, body, boots, gloves, grimoire, amulet, belt,
				rings.get(0), rings.get(1),
				weapons.get(0), weapons.get(1)
		};

		return Arrays.stream(gears)
				.filter(Objects::nonNull)
				.iterator();
	}
}
