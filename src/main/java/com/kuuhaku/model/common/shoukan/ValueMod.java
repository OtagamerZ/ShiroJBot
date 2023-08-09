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

import java.util.Objects;

public class ValueMod implements Cloneable {
	private final Drawable<?> source;
	private double value;
	private int expiration;

	private final Side side;
	private final int hash;

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

		if (source instanceof Evogear e) {
			this.hash = e.posHash();
		} else if (source instanceof Senshi s) {
			this.hash = s.posHash();
		} else if (source != null && source.getHand() != null) {
			this.hash = source.getSlot().hashCode();
		} else {
			this.hash = -1;
		}
	}

	public Drawable<?> getSource() {
		return source;
	}

	public double getValue() {
		return value;
	}

	public <T extends Number> T asType(Class<T> klass) {
		if (klass == Short.class) {
			return klass.cast((short) value);
		} else if (klass == Integer.class) {
			return klass.cast((int) value);
		} else if (klass == Long.class) {
			return klass.cast((long) value);
		} else if (klass == Float.class) {
			return klass.cast((float) value);
		} else if (klass == Double.class) {
			return klass.cast(value);
		} else if (klass == Byte.class) {
			return klass.cast((byte) value);
		}

		throw new ClassCastException();
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
			if (source instanceof Evogear e && !e.isSpell() && (e.getEquipper() == null || e.posHash() != hash)) {
				return true;
			} else if (source instanceof Senshi s && s.posHash() != hash) {
				return true;
			}
		}

		return value == 0 || expiration == 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ValueMod valueMod = (ValueMod) o;
		return hash == valueMod.hash && Objects.equals(source, valueMod.source);
	}

	@Override
	public int hashCode() {
		return Objects.hash(source, hash);
	}

	@Override
	public ValueMod clone() {
		try {
			return (ValueMod) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}
}
