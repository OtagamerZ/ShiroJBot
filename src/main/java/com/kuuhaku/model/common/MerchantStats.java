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

public class MerchantStats {
	private final String uid;
	private final int month;
	private final double sold;
	private final double uniqueBuyers;

	private MerchantStats(Object uid, Object month, Object sold, Object uniqueBuyers) {
		this.uid = (String) uid;
		this.month = ((Number) month).intValue();
		this.sold = ((Number) sold).doubleValue();
		this.uniqueBuyers = ((Number) uniqueBuyers).doubleValue();
	}

	public static MerchantStats of(Object[] values) {
		return new MerchantStats(values[0], values[1], values[2], values[3]);
	}

	public String getUid() {
		return uid;
	}

	public int getMonth() {
		return month;
	}

	public double getSold() {
		return sold;
	}

	public double getUniqueBuyers() {
		return uniqueBuyers;
	}
}
