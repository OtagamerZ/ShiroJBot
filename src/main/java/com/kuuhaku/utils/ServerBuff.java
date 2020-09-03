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

package com.kuuhaku.utils;

import org.apache.commons.lang3.tuple.Triple;

import java.util.Objects;

public class ServerBuff {
	public static final Triple<Integer, Integer, Double> XP_TIER_1 = Triple.of(1, 1500, 1.5);
	public static final Triple<Integer, Integer, Double> XP_TIER_2 = Triple.of(1, 4000, 1.75);
	public static final Triple<Integer, Integer, Double> XP_TIER_3 = Triple.of(1, 10000, 2d);
	public static final Triple<Integer, Integer, Double> CARD_TIER_1 = Triple.of(2, 1000, 1.2);
	public static final Triple<Integer, Integer, Double> CARD_TIER_2 = Triple.of(2, 3000, 1.3);
	public static final Triple<Integer, Integer, Double> CARD_TIER_3 = Triple.of(2, 5000, 1.4);
	public static final Triple<Integer, Integer, Double> CARD_TIER_U = Triple.of(2, 50000, 0d);
	public static final Triple<Integer, Integer, Double> DROP_TIER_1 = Triple.of(3, 1250, 1.2);
	public static final Triple<Integer, Integer, Double> DROP_TIER_2 = Triple.of(3, 3500, 1.3);
	public static final Triple<Integer, Integer, Double> DROP_TIER_3 = Triple.of(3, 6000, 1.4);
	public static final Triple<Integer, Integer, Double> DROP_TIER_U = Triple.of(3, 60000, 0d);
	public static final Triple<Integer, Integer, Double> FOIL_TIER_1 = Triple.of(4, 5000, 1.2);
	public static final Triple<Integer, Integer, Double> FOIL_TIER_2 = Triple.of(4, 8000, 1.5);
	public static final Triple<Integer, Integer, Double> FOIL_TIER_3 = Triple.of(4, 12000, 2d);
	public static final Triple<Integer, Integer, Double> FOIL_TIER_U = Triple.of(4, 120000, 0d);

	private int tier;
	private int id;
	private int price;
	private int time;
	private double mult;
	private long acquiredAt;

	public ServerBuff(int tier, int id, int price, double mult) {
		this.tier = tier;
		this.id = id;
		this.price = price;
		this.time = tier == 1 ? 15 : tier == 2 ? 10 : tier == 3 ? 5 : 1;
		this.mult = mult;
		this.acquiredAt = System.currentTimeMillis();
	}

	public ServerBuff(int tier, Triple<Integer, Integer, Double> values) {
		this.tier = tier;
		this.id = values.getLeft();
		this.price = values.getMiddle();
		this.time = tier == 1 ? 15 : tier == 2 ? 10 : tier == 3 ? 5 : 1;
		this.mult = values.getRight();
		this.acquiredAt = System.currentTimeMillis();
	}

	public ServerBuff() {
	}

	public int getTier() {
		return tier;
	}

	public void setTier(int tier) {
		this.tier = tier;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		this.price = price;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public double getMult() {
		return mult;
	}

	public void setMult(float mult) {
		this.mult = mult;
	}

	public long getAcquiredAt() {
		return acquiredAt;
	}

	public void setAcquiredAt(long acquiredAt) {
		this.acquiredAt = acquiredAt;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ServerBuff that = (ServerBuff) o;
		return id == that.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
