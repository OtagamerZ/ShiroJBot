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

package com.kuuhaku.model.common.shoukan;

import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.utils.Utils;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

public class Arena {
	private final int WIDTH = (225 * 5 /* slots */) + (225 * 2 /* side stacks */) + (50 * (5 + 2) /* margin */);
	private final int HEIGHT = (350 * 2 /* slots */) + (100 * 2 /* effects */) + (50 * (2 + 2) /* margin */);

	private final Map<Side, List<SlotColumn>> slots;

	public Arena() {
		slots = Map.of(
				Side.TOP, Utils.generate(5, i -> new SlotColumn(Side.TOP, i)),
				Side.BOTTOM, Utils.generate(5, i -> new SlotColumn(Side.BOTTOM, i))
		);
	}

	public Map<Side, List<SlotColumn>> getSlots() {
		return slots;
	}

	public List<SlotColumn> getSlots(Side side) {
		return slots.get(side);
	}

	public BufferedImage render() {

	}
}
