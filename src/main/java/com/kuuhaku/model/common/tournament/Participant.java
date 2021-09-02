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

package com.kuuhaku.model.common.tournament;

import com.kuuhaku.utils.Helper;

import java.util.Objects;

public class Participant {
	private final String id;
	private int index = -1;
	private int wins = 0;
	private boolean third = false;

	public Participant(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getPoints() {
		return wins;
	}

	public boolean isWinner(int phase) {
		return (wins & 1 << phase) != 0;
	}

	public void won(int phase) {
		this.wins |= 1 << phase;
	}

	public boolean isThird() {
		return third;
	}

	public void setThird() {
		this.third = true;
	}

	public boolean isBye() {
		return id == null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Participant that = (Participant) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return isBye() ? "BYE" : Helper.getUsername(id);
	}
}
