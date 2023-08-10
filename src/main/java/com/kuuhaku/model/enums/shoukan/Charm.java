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

package com.kuuhaku.model.enums.shoukan;

import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;

import java.awt.image.BufferedImage;

public enum Charm {
	SHIELD,
	PIERCING,
	WOUNDING,
	DRAIN,
	CLONE,
	WARDING,
	TIMEWARP,
	THORNS,
	LIFESTEAL,
	BARRAGE;

	public String getName(I18N locale) {
		return locale.get("charm/" + name());
	}

	public String getDescription(I18N locale) {
		return locale.get("charm/" + name() + "_desc");
	}

	public String getDescription(I18N locale, int tier) {
		String val;
		if (Utils.equalsAny(this, WARDING, TIMEWARP)) {
			val = locale.get("str/" + getValue(tier) + "_time").toLowerCase();
		} else {
			val = String.valueOf(getValue(tier));
		}

		return locale.get("charm/" + name() + "_desc", val);
	}

	public BufferedImage getIcon() {
		return IO.getResourceAsImage("shoukan/charm/" + name().toLowerCase() + ".png");
	}

	public int getValue(int tier) {
		tier = Math.max(1, tier);
		return switch (this) {
			case DRAIN, WARDING, TIMEWARP -> (int) Calc.getFibonacci(tier);
			case SHIELD -> (int) Calc.getFibonacci(tier) + 1;
			case PIERCING, WOUNDING, THORNS, LIFESTEAL -> tier * 4;
			case CLONE -> tier * 25;
			case BARRAGE -> tier * 3;
		};
	}
}
