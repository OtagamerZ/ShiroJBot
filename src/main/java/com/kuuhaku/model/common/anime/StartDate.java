/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.common.anime;

import java.time.LocalDate;

public class StartDate {
	private int year;
	private int month;
	private int day;

	public int getYear() {
		return year;
	}

	public void setYear(int value) {
		this.year = value;
	}

	public int getMonth() {
		return month;
	}

	public void setMonth(int value) {
		this.month = value;
	}

	public int getDay() {
		return day;
	}

	public void setDay(int value) {
		this.day = value;
	}

	public LocalDate getDate() {
		return LocalDate.of(year, month, day);
	}
}
