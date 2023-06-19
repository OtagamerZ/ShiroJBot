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

import com.kuuhaku.util.Utils;

public abstract class ValueOverTime {
	private final int baseValue;
	private final double multiplier;

	private int value;

	public ValueOverTime(int value, double multiplier) {
		this.baseValue = this.value = Math.max(0, value);
		this.multiplier = Utils.clamp(multiplier, 0, 1);
	}

	public int getBaseValue() {
		return baseValue;
	}

	public double getMultiplier() {
		return multiplier;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public int reduce(int value) {
		if (value > this.value) {
			value -= this.value;
			this.value = 0;
		} else {
			this.value -= value;
			value = 0;
		}

		return value;
	}

	public int next() {
		int val = (int) Math.min(baseValue * multiplier, value);
		value -= val;

		if (this instanceof Regen) {
			return val;
		} else {
			return -val;
		}
	}

	public int peek() {
		if (this instanceof Regen) {
			return value;
		} else {
			return -value;
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + value + " v/t";
	}
}
