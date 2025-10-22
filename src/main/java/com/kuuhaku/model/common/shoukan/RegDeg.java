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

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

public class RegDeg {
	@Nullable
	private transient final Hand parent;
	private final BondedList<ValueOverTime> values = BondedList.withBind((v, it) -> {
		v.setValue(reduce(v.getClass(), v.getValue()));
		return true;
	});

	public RegDeg(@Nullable Hand parent) {
		this.parent = parent;
	}

	public BondedList<ValueOverTime> getValues() {
		return values;
	}

	public void add(Number val) {
		add(val, val.doubleValue() < 0 ? 1 : 0.2);
	}

	public void add(Number val, double mult) {
		if (val == null) return;
		else if (parent != null && parent.getOrigins().synergy() == Race.CONDEMNED) return;
		int value = val.intValue();

		if (value < 0) {
			if (parent != null) {
				if (Utils.equalsAny(parent.getOrigins().major(), Race.HUMAN, Race.UNDEAD)) {
					value /= 2;
				}

				if (parent.getGame().getArcade() == Arcade.DECAY) {
					mult *= 1.5;
				}

				mult = parent.getStats().getDegenMult().apply(mult);
				if (parent.getOther().getOrigins().synergy() == Race.GHOUL) {
					int split = -value / 2;
					values.add(new Degen(split, mult));
					values.add(new Degen(split, mult));
					return;
				}
			}

			values.add(new Degen(-value, mult));
		} else if (value > 0) {
			if (parent != null) {
				if (parent.getOrigins().major() == Race.HUMAN) {
					if (parent.getOrigins().isPure()) {
						parent.modHP(val.intValue());
						return;
					}

					mult += 0.2;
				}

				if (parent.getGame().getArcade() == Arcade.DECAY) {
					mult /= 2;
				}

				mult = parent.getStats().getRegenMult().apply(mult);
			}

			values.add(new Regen(value, mult));
		}
	}

	public void leftShift(Number val) {
		add(val);
	}

	public void leftShift(List<Number> val) {
		add(val.getFirst(), val.getLast().doubleValue());
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
		if (parent == null) return;

		int value = (int) (peek() * Utils.clamp(prcnt, 0, 1));
		parent.modHP(value);

		if (value > 0) {
			reduce(Degen.class, value);
		} else {
			reduce(Regen.class, value);
		}
	}

	public int next() {
		try {
			int virus = 0;
			if (parent != null && parent.getOrigins().synergy() == Race.VIRUS) {
				virus = -Math.min(parent.getOther().getRegDeg().peek(), 0);
			}

			return values.stream().mapToInt(ValueOverTime::next).sum() + virus;
		} finally {
			values.removeIf(v -> v.getValue() <= 0);

			Iterator<ValueOverTime> it = values.iterator();
			while (it.hasNext()) {
				ValueOverTime vot = it.next();
				if (vot instanceof Degen) {
					if (parent != null && parent.getGame().getCurrent().getOrigins().synergy() == Race.FIEND && parent.getGame().getRng().nextBoolean()) {
						break;
					}

					it.remove();
					break;
				}
			}
		}
	}

	public int peek() {
		int virus = 0;
		if (parent != null) {
			if (parent.getOrigins().synergy() == Race.VIRUS && parent.getOther().getOrigins().synergy() != Race.VIRUS) {
				virus = -Math.min(parent.getOther().getRegDeg().peek(), 0);
			}
		}

		return values.stream().mapToInt(ValueOverTime::peek).sum() + virus;
	}

	public void clear() {
		values.clear();
	}

	public <T extends ValueOverTime> void clear(Class<T> klass) {
		values.removeIf(vot -> vot.getClass() == klass);
	}
}
