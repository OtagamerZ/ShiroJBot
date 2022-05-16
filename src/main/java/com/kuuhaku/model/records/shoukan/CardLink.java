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

package com.kuuhaku.model.records.shoukan;

import com.kuuhaku.Constants;
import com.kuuhaku.interfaces.Drawable;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.utils.Utils;

import java.util.concurrent.atomic.AtomicInteger;

public record CardLink(AtomicInteger index, Drawable linked, Drawable self) {
	public int getIndex() {
		return index.get();
	}

	public Senshi asSenshi() {
		if (linked instanceof Senshi s)
			return s;

		throw new ClassCastException("Wrong Drawable type: " + linked.getClass().getSimpleName() + " " + linked.getCard().getName() + ".");
	}

	public Evogear asEvogear() {
		if (linked instanceof Evogear e)
			return e;

		throw new ClassCastException("Wrong Drawable type: " + linked.getClass().getSimpleName() + " " + linked.getCard().getName() + ".");
	}

	public CardType getType() {
		if (linked instanceof Senshi)
			return CardType.KAWAIPON;
		else
			return CardType.EVOGEAR;
	}

	public boolean isFake() {
		return getIndex() == -1;
	}

	public boolean isInvalid() {
		try {
			if (linked == null || (!Utils.between(getIndex(), 0, 5) && getIndex() != -1)) return true;
			else if (linked.getSide() != self.getSide()) return true;
			else if (isFake()) return false;

			/*SlotColumn sc = linked.getGame().getSlot(linked.getSide(), getIndex());
			Drawable d;
			if (linked instanceof Senshi) {
				d = sc.getTop();
			} else {
				d = sc.getBottom();
			}

			return !linked.equals(d);*/
			return false;
		} catch (IndexOutOfBoundsException e) {
			Constants.LOGGER.error(e + ": [" + getIndex() + ", " + linked.getCard().getId() + "]");
			return true;
		}
	}

	public void sync() {
		/*if (linked instanceof Senshi s) {
			s.link((Evogear) self);
		} else {
			((Evogear) linked).link((Senshi) self);
		}*/
	}

	@Override
	public String toString() {
		return index.get() + "-" + linked;
	}
}