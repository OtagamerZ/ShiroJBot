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

import com.kuuhaku.model.common.BondedList;
import com.kuuhaku.model.enums.shoukan.Arcade;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.util.Utils;

import java.util.List;

public class RegDeg {
	private final Hand parent;
	private final BondedList<ValueOverTime> values = BondedList.withBind((v, it) -> {
		v.setValue(reduce(v.getClass(), v.getValue()));
		return true;
	});

	public RegDeg(Hand parent) {
		this.parent = parent;
	}

	public List<ValueOverTime> getValues() {
		return values;
	}

	public void add(Number val) {
		add(val, 0.2);
	}

	public void add(Number val, double mult) {
		if (val == null) return;
		int value = val.intValue();

		if (value < 0) {
			if (parent.getOrigin().major() == Race.HUMAN) {
				mult += 0.2;
			}

			if (parent.getGame().getArcade() == Arcade.DECAY) {
				mult *= 1.5;
			}

			values.add(new Degen(-value, mult * parent.getStats().getDegenMult().get()));
		} else if (value > 0) {
			if (parent.getOrigin().major() == Race.HUMAN) {
				mult -= 0.1;
			}

			if (parent.getGame().getArcade() == Arcade.DECAY) {
				mult /= 2;
			}

			values.add(new Regen(value, mult * parent.getStats().getRegenMult().get()));
		}
	}

	public void leftShift(Number val) {
		add(val);
	}

	public void leftShift(List<Number> val) {
		add(val.get(0), val.get(1).doubleValue());
	}

	public <T extends ValueOverTime> int reduce(Class<T> klass, int val) {
		if (val == 0) return 0;
		else if (val < 0) val = -val;

		for (ValueOverTime vot : values) {
			if (!vot.getClass().equals(klass)) {
				if ((val = vot.reduce(val)) <= 0) {
					break;
				}
			}
		}

		return val;
	}

	public void apply(double prcnt) {
		int value = (int) (peek() * Utils.clamp(prcnt, 0, 1));
		parent.modHP(value);

		if (value > 0) {
			reduce(Regen.class, value);
		} else {
			reduce(Degen.class, value);
		}
	}

	public int next() {
		try {
			return values.stream().mapToInt(ValueOverTime::next).sum();
		} finally {
			values.removeIf(v -> v.getValue() <= 0);
		}
	}

	public int peek() {
		return values.stream().mapToInt(ValueOverTime::peek).sum();
	}

	public <T extends ValueOverTime> void clear() {
		values.clear();
	}

	public <T extends ValueOverTime> void clear(Class<T> klass) {
		values.removeIf(vot -> vot.getClass() == klass);
	}
}
