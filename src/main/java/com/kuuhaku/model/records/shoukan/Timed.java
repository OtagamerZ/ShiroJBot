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

package com.kuuhaku.model.records.shoukan;

public final class Timed<T> {
	private final T value;
	private int time;

	public Timed(T value, int time) {
		this.value = value;
		this.time = time;
	}

	public T getValue() {
		return value;
	}

	public int getTime() {
		return time;
	}

	public int addTime(int extra) {
		return time += extra;
	}

	public int reduceTime(int extra) {
		return time -= extra;
	}

	public void setTime(int time) {
		this.time = time;
	}
}
