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

import com.kuuhaku.util.Bit32;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record Attributes(@Column(name = "attributes", nullable = false) int attributes) {
	public Attributes(int str, int dex, int wis, int vit) {
		this((Math.max(0, str) & 0xFF)
			 | (Math.max(0, dex) & 0xFF) << 8
			 | (Math.max(0, wis) & 0xFF) << 16
			 | (Math.max(0, vit) & 0xFF) << 24
		);
	}
	/*
	0xFF FF FF FF
	  └┤ └┤ └┤ └┴ strength
	   │  │  └ dexterity
	   │  └ wisdom
	   └ vitality
	 */

	public int str() {
		return Bit32.get(attributes, 0, 8);
	}

	public int dex() {
		return Bit32.get(attributes, 1, 8);
	}

	public int wis() {
		return Bit32.get(attributes, 2, 8);
	}

	public int vit() {
		return Bit32.get(attributes, 3, 8);
	}

	public Attributes merge(Attributes attr) {
		return new Attributes(
				str() + attr.str(),
				dex() + attr.dex(),
				wis() + attr.wis(),
				vit() + attr.vit()
		);
	}

	public int count() {
		return str() + dex() + wis() + vit();
	}
}
