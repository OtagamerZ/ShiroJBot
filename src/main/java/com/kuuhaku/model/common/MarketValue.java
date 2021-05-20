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

package com.kuuhaku.model.common;

import java.sql.Timestamp;
import java.util.Date;

public class MarketValue {
	private final String id;
	private final int open;
	private final int high;
	private final int low;
	private final int close;
	private final Timestamp date;

	public MarketValue(String id, int open, int high, int low, int close, Timestamp date) {
		this.id = id;
		this.open = open;
		this.high = high;
		this.low = low;
		this.close = close;
		this.date = date;
	}

	public String getId() {
		return id;
	}

	public int getOpen() {
		return open;
	}

	public int getHigh() {
		return high;
	}

	public int getLow() {
		return low;
	}

	public int getClose() {
		return close;
	}

	public Date getDate() {
		return date;
	}
}
