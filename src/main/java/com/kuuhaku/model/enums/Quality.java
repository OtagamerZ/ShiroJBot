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

package com.kuuhaku.model.enums;

import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.kuuhaku.util.IO;

public enum Quality {
	NORMAL, FINE, POLISHED, FLAWLESS;

	public byte[] getOverlayBytes() {
		return Main.getCacheManager().computeResource("quality_" + name(), (k, v) -> {
			if (v != null && v.length > 0) return v;

			return IO.getBytes(IO.getImage(Constants.API_ROOT + "quality/" + name() + ".png"), "png");
		});
	}

	public static Quality get(double quality) {
		return values()[(int) Math.min(quality * (values().length - 1) / 18, values().length - 1)];
	}
}
