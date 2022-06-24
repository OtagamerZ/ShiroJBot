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

package com.kuuhaku.model.enums.shoukan;

import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.utils.Calc;
import com.kuuhaku.utils.IO;
import com.kuuhaku.utils.Utils;

import java.awt.image.BufferedImage;
import java.util.Locale;

public enum Charm {
	SHIELD,
	PIERCING,
	WOUNDING,
	DRAIN,
	CLONE,
	AGILITY,
	ARMOR,
	WARDING,
	TIMEWARP,
	DOUBLETAP;

	public String getName(I18N locale) {
		return locale.get("charm/" + name());
	}

	public String getDescription(I18N locale) {
		return locale.get("charm/" + name() + "_desc");
	}

	public String getDescription(I18N locale, int tier) {
		String val;
		if (Utils.equalsAny(this, WARDING, TIMEWARP, DOUBLETAP)) {
			val = locale.get("str/" + getValue(tier) + "_time");
		} else {
			val = String.valueOf(getValue(tier));
		}

		return locale.get("charm/" + name() + "_desc").formatted(val);
	}

	public BufferedImage getIcon() {
		return IO.getResourceAsImage("shoukan/charm/" + name().toLowerCase(Locale.ROOT) + ".png");
	}

	public int getValue(int tier) {
		return switch (this) {
			case SHIELD, DRAIN, WARDING, TIMEWARP, DOUBLETAP -> (int) Calc.getFibonacci(tier);
			case PIERCING, WOUNDING, ARMOR -> tier * 4;
			case CLONE -> tier * 25;
			case AGILITY -> tier * 7;
		};
	}
}
