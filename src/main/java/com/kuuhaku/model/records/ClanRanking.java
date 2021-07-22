/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.records;

import com.kuuhaku.utils.Helper;

import java.awt.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

public record ClanRanking(int id, String name, BigInteger score, String icon) {
	public Color getColor() {
		if (icon != null)
			return Helper.colorThief(Helper.btoa(icon));

		CRC32 crc = new CRC32();
		crc.update(name.getBytes(StandardCharsets.UTF_8));
		return Color.decode("#%02X%02X%02X".formatted(crc.getValue() & 0xFFFFFF));
	}
}
