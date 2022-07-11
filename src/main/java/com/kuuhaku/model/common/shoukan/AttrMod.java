/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class AttrMod {
	private final Drawable<?> source;
	private final double value;
	private final AtomicInteger expiration;

	private final int hash;

	protected AttrMod(double value) {
		this.source = null;
		this.value = value;
		this.expiration = null;
		this.hash = -1;
	}

	public AttrMod(Drawable<?> source, double value) {
		this.source = source;
		this.value = value;
		this.expiration = null;
		this.hash = source.getSlot().validationHash();
	}

	public AttrMod(Drawable<?> source, double value, int expiration) {
		this.source = source;
		this.value = value;
		this.expiration = new AtomicInteger(expiration);
		this.hash = source.getSlot().validationHash();
	}

	public Drawable<?> getSource() {
		return source;
	}

	public double getValue() {
		return value;
	}

	public AtomicInteger getExpiration() {
		return expiration;
	}

	public boolean isExpired() {
		if (hash != -1) {
			if (source.getSlot() == null || source.getSlot().validationHash() != hash) {
				return true;
			}
		}

		return expiration != null && expiration.get() <= 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		AttrMod attrMod = (AttrMod) o;
		return hash == attrMod.hash && Objects.equals(source, attrMod.source);
	}

	@Override
	public int hashCode() {
		return Objects.hash(source, hash);
	}
}
