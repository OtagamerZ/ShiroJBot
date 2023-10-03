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

package com.kuuhaku;

import java.util.Arrays;
import java.util.Comparator;

public class Scalable extends Value {
	private Value[] values = new Value[2];
	private String delimiter;
	private boolean grouped;

	public Value set(int pos, Value value) {
		if (pos == 0) {
			return setLeft(value);
		} else {
			return setRight(value);
		}
	}

	public Value setLeft(Value left) {
		return values[0] = left;
	}

	public Value setRight(Value right) {
		return values[1] = right;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	public void setGrouped(boolean grouped) {
		this.grouped = grouped;
	}

	public void invert() {
		for (Value v : values) {
			if (v instanceof Invertable i) {
				i.invert();
				break;
			}
		}
	}

	public boolean isScalingVar() {
		int vars = 0;
		int scales = 0;

		for (Value v : values) {
			if (v instanceof VariableValue) {
				vars++;
			} else if (v instanceof PercentageValue) {
				scales++;
			}
		}

		return vars == 1 && scales == 1;
	}

	public boolean isPure() {
		int vals = 0;
		for (Value v : values) {
			if (v != null) vals++;
		}

		return vals <= 1;
	}

	@Override
	public String toString() {
//		if (isPure()) {
//			for (Value v : values) {
//				if (v != null) return String.valueOf(v);
//			}
//		}

		StringBuilder delimiter = new StringBuilder();
		delimiter.append(this.delimiter);

		String out;
		if (isScalingVar()) {
			Arrays.sort(values, Comparator.comparing(v -> !(v instanceof VariableValue)));
			out = values[1] + "" + values[0];
		} else {
			for (int i = 0; i < values.length; i++) {
				Value v = values[i];

				if (!(v instanceof VariableValue)) {
					if (i == 0) {
						delimiter.insert(0, " ");
					} else {
						delimiter.append(" ");
					}

					if (v instanceof PercentageValue p) {
						p.setFlat(true);
					}
				}
			}

			out = String.join(delimiter.toString(), String.valueOf(values[0]), String.valueOf(values[1]));
		}

		if (grouped) {
			out = "(" + out + ")";
		}

		return out;
	}
}
