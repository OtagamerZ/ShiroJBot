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

package com.kuuhaku.model.records.dunhun;

import com.kuuhaku.model.enums.dunhun.AttrType;
import com.kuuhaku.util.Bit32;
import com.kuuhaku.util.Calc;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import kotlin.Pair;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

@Embeddable
public record Attributes(@Column(name = "attributes", nullable = false) int attributes) implements Serializable {
	public Attributes() {
		this(0);
	}

	public Attributes(int str, int dex, int wis, int vit) {
		this((Calc.clamp(str, Byte.MIN_VALUE, Byte.MAX_VALUE) & 0xFF)
			 | (Calc.clamp(dex, Byte.MIN_VALUE, Byte.MAX_VALUE) & 0xFF) << 8
			 | (Calc.clamp(wis, Byte.MIN_VALUE, Byte.MAX_VALUE) & 0xFF) << 16
			 | (Calc.clamp(vit, Byte.MIN_VALUE, Byte.MAX_VALUE) & 0xFF) << 24
		);
	}
	/*
	0xFF FF FF FF
	  └┤ └┤ └┤ └┴ strength
	   │  │  └ dexterity
	   │  └ wisdom
	   └ vitality
	 */

	public byte str() {
		return (byte) Bit32.get(attributes, 0, 8);
	}

	public byte dex() {
		return (byte) Bit32.get(attributes, 1, 8);
	}

	public byte wis() {
		return (byte) Bit32.get(attributes, 2, 8);
	}

	public byte vit() {
		return (byte) Bit32.get(attributes, 3, 8);
	}

	public byte get(AttrType type) {
		return switch (type) {
			case STR -> str();
			case DEX -> dex();
			case WIS -> wis();
			case VIT -> vit();
			default -> 0;
		};
	}

	public Attributes set(AttrType type, int value) {
		return merge(switch (type) {
			case STR -> new Attributes(value, 0, 0, 0);
			case DEX -> new Attributes(0, value, 0, 0);
			case WIS -> new Attributes(0, 0, value, 0);
			case VIT -> new Attributes(0, 0, 0, value);
			default -> new Attributes();
		});
	}

	public Attributes plus(Attributes other) {
		return merge(other);
	}

	public Attributes minus(Attributes other) {
		return merge(other);
	}

	public Attributes merge(Attributes... attrs) {
		return merge(List.of(attrs));
	}

	public Attributes merge(Collection<Attributes> attrs) {
		Attributes out = this;
		for (Attributes a : attrs) {
			out = new Attributes(
					out.str() + a.str(),
					out.dex() + a.dex(),
					out.wis() + a.wis(),
					out.vit() + a.vit()
			);
		}

		return out;
	}

	public Attributes reduce(Attributes... attrs) {
		return reduce(List.of(attrs));
	}

	public Attributes reduce(Collection<Attributes> attrs) {
		Attributes out = this;
		for (Attributes a : attrs) {
			out = new Attributes(
					out.str() - a.str(),
					out.dex() - a.dex(),
					out.wis() - a.wis(),
					out.vit() - a.vit()
			);
		}

		return out;
	}

	public Attributes modify(Pair<Attributes, Attributes> attrs) {
		return modify(attrs.getFirst(), attrs.getSecond());
	}

	public Attributes modify(Attributes add, Attributes sub) {
		return new Attributes(
				str() + add.str() - sub.str(),
				dex() + add.dex() - sub.dex(),
				wis() + add.wis() - sub.wis(),
				vit() + add.vit() - sub.vit()
		);
	}

	public boolean has(Attributes attr) {
		for (AttrType a : AttrType.values()) {
			if (a == AttrType.LVL) break;
			if (attr.get(a) > 0 && get(a) < attr.get(a)) return false;
		}

		return true;
	}

	public int count() {
		return str() + dex() + wis() + vit();
	}

	@Override
	public String toString() {
		return "Attributes[str=" + str() + ", dex=" + dex() + ", wis=" + wis() + ", vit=" + vit() + "]";
	}
}
