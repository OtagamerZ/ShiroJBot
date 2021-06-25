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

package com.kuuhaku.handlers.games.tabletop.framework.enums;

public enum BoardSize {
	S_3X3(new int[]{3, 3}),
	S_8X8(new int[]{8, 8}),
	S_NONE(new int[]{0, 0});

	private final int[] dimensions;

	BoardSize(int[] dimensions) {
		this.dimensions = dimensions;
	}

	public int[] getDimensions() {
		return dimensions;
	}

	public int getHeight() {
		return dimensions[0];
	}

	public int getWidth() {
		return dimensions[1];
	}
}
