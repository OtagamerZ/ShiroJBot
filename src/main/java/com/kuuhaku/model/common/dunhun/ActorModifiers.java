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

import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.common.shoukan.CumValue;
import com.kuuhaku.model.common.shoukan.ValueMod;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.records.dunhun.Attributes;

import java.lang.reflect.Field;
import java.util.function.Predicate;

public class ActorModifiers {
	private final CumValue maxHp = CumValue.flat();
	private final CumValue hpMult = CumValue.mult();
	private final CumValue maxAp = CumValue.flat();
	private final CumValue initiative = CumValue.flat();
	private final CumValue critical = CumValue.flat();
	private final CumValue aggroMult = CumValue.flat();

	private final CumValue str = CumValue.flat();
	private final CumValue dex = CumValue.flat();
	private final CumValue wis = CumValue.flat();
	private final CumValue vit = CumValue.flat();

	private Actor channeled;

	private Field[] fieldCache = null;

	public CumValue getMaxHp() {
		return maxHp;
	}

	public CumValue getHpMult() {
		return hpMult;
	}

	public CumValue getMaxAp() {
		return maxAp;
	}

	public CumValue getInitiative() {
		return initiative;
	}

	public CumValue getCritical() {
		return critical;
	}

	public CumValue getAggroMult() {
		return aggroMult;
	}

	public CumValue getStrength() {
		return str;
	}

	public CumValue getDexterity() {
		return dex;
	}

	public CumValue getWisdom() {
		return wis;
	}

	public CumValue getVitality() {
		return vit;
	}

	public Attributes getAttributes() {
		return new Attributes(
				(int) getStrength().get(),
				(int) getDexterity().get(),
				(int) getWisdom().get(),
				(int) getVitality().get()
		);
	}

	public Actor getChanneled() {
		return channeled;
	}

	public void setChanneled(Actor channeled) {
		this.channeled = channeled;
	}

	public void expireMods(Senshi sen) {
		sen.getStats().expireMods();

		removeIf(mod -> {
			if (mod.getExpiration() > 0) {
				mod.decExpiration();
			}

			return mod.isExpired();
		});
	}

	public void clear() {
		removeIf(o -> true);
	}

	public void removeIf(Predicate<ValueMod> check) {
		if (fieldCache == null) {
			fieldCache = getClass().getDeclaredFields();
		}

		for (Field f : fieldCache) {
			try {
				if (f.get(this) instanceof CumValue cv) {
					cv.values().removeIf(check);
				}
			} catch (IllegalAccessException ignore) {
			}
		}
	}
}
