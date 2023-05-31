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

package com.kuuhaku.model.records.shoukan.snapshot;

import com.kuuhaku.model.common.shoukan.SlotColumn;
import com.kuuhaku.util.IO;
import com.ygimenez.json.JSONUtils;

import java.io.IOException;

public record Slot(byte[] top, byte[] equips, byte[] bottom, byte state) {
	public Slot(SlotColumn slt) throws IOException {
		this(
				JSONUtils.toJSON(slt.getTop()),
				JSONUtils.toJSON(slt.getTop() == null ? null : slt.getTop().getEquipments()),
				JSONUtils.toJSON(slt.getBottom()),
				slt.getState()
		);
	}

	private Slot(String top, String equips, String bottom, byte state) throws IOException {
		this(IO.compress(top), IO.compress(equips), IO.compress(bottom), state);
	}
}
