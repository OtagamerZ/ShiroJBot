/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.handlers.games.tabletop.games.shoukan;

public class SlotColumn {
	private final int index;
	private Champion top = null;
	private Equipment bottom = null;
	private int unavailable = 0;

	public SlotColumn(int index) {
		this.index = index;
	}

	public SlotColumn(int index, Champion top, Equipment bottom) {
		this.index = index;
		this.top = top;
		this.bottom = bottom;
	}

	public Champion getTop() {
		return top;
	}

	public void setTop(Champion top) {
		if (top != null) top.setIndex(index);
		this.top = top;
	}

	public Equipment getBottom() {
		return bottom;
	}

	public void setBottom(Equipment bottom) {
		if (bottom != null) bottom.setIndex(index);
		this.bottom = bottom;
	}

	public int getIndex() {
		return index;
	}

	public void setUnavailable(int time) {
		this.unavailable = Math.max(0, this.unavailable + time);
	}

	public boolean isUnavailable() {
		return unavailable > 0;
	}
}
