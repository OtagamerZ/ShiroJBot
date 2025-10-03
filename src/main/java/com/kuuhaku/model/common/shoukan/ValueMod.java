/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.common.shoukan;

import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.util.Utils;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class ValueMod implements Cloneable {
	private final int SERIAL = ThreadLocalRandom.current().nextInt();
	private final Drawable<?> source;
	private final boolean permanent;
	private double value;
	private int expiration;

	private final Side side;

	protected ValueMod(double value) {
		this(null, value);
	}

	public ValueMod(Drawable<?> source, double value) {
		this(source, value, -1);
	}

	public ValueMod(Drawable<?> source, double value, int expiration) {
		this.source = source;
		this.value = value;
		this.expiration = expiration;
		this.side = source == null ? null : source.getSide();

		permanent = source == null && expiration == -1;
	}

	public Drawable<?> getSource() {
		return source;
	}

	public double getValue() {
		return value;
	}

	public <T> T asType(Class<T> klass) {
		if (ValueMod.class.isAssignableFrom(klass)) return klass.cast(this);

		return Utils.fromNumber(klass, value);
	}

	public void setValue(double value) {
		this.value = value;
	}

	public int getExpiration() {
		return expiration;
	}

	public void decExpiration() {
		this.expiration--;
	}

	public boolean isExpired() {
		if (side != null) {
			if (source instanceof Evogear e && !e.isSpell() && (e.getEquipper() == null || e.getSide() != side)) {
				return true;
			} else if (source instanceof Senshi s && s.getSide() != side) {
				return true;
			}
		}

		return value == 0 || expiration == 0;
	}

	public boolean isPermanent() {
		return permanent;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ValueMod valueMod = (ValueMod) o;
		if (source == null) return SERIAL == valueMod.SERIAL;

		return Objects.equals(source, valueMod.source) && side == valueMod.side;
	}

	@Override
	public int hashCode() {
		if (source == null) return SERIAL;
		return Objects.hash(source, side);
	}

	public ValueMod copy() {
		try {
			return (ValueMod) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}
}
