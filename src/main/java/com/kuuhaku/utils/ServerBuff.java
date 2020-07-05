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
	public static final Triple<Integer, Integer, Float> XP_TIER_1 = Triple.of(1, 1500, 1.5f);
	public static final Triple<Integer, Integer, Float> XP_TIER_2 = Triple.of(1, 4000, 2f);
	public static final Triple<Integer, Integer, Float> XP_TIER_3 = Triple.of(1, 10000, 3f);
	public static final Triple<Integer, Integer, Float> CARD_TIER_1 = Triple.of(2, 1000, 0.8f);
	public static final Triple<Integer, Integer, Float> CARD_TIER_2 = Triple.of(2, 3000, 0.7f);
	public static final Triple<Integer, Integer, Float> CARD_TIER_3 = Triple.of(2, 5000, 0.6f);
	public static final Triple<Integer, Integer, Float> DROP_TIER_1 = Triple.of(3, 1250, 0.8f);
	public static final Triple<Integer, Integer, Float> DROP_TIER_2 = Triple.of(3, 3500, 0.7f);
	public static final Triple<Integer, Integer, Float> DROP_TIER_3 = Triple.of(3, 6000, 0.6f);
	public static final Triple<Integer, Integer, Float> FOIL_TIER_1 = Triple.of(4, 5000, 1.2f);
	public static final Triple<Integer, Integer, Float> FOIL_TIER_2 = Triple.of(4, 8000, 1.5f);
	public static final Triple<Integer, Integer, Float> FOIL_TIER_3 = Triple.of(4, 12000, 2f);

	private int tier;
	private int id;
	private int price;
	private int time;
	private float mult;
	private long acquiredAt;

	public ServerBuff(int tier, int id, int price, float mult) {
		this.tier = tier;
		this.id = id;
		this.price = price;
		this.time = tier == 1 ? 30 : tier == 2 ? 15 : 7;
		this.mult = mult;
		this.acquiredAt = System.currentTimeMillis();
	}

	public ServerBuff(int tier, Triple<Integer, Integer, Float> values) {
		this.tier = tier;
		this.id = values.getLeft();
		this.price = values.getMiddle();
		this.time = tier == 1 ? 30 : tier == 2 ? 15 : 7;
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

	public float getMult() {
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
	public int hashCode() {
		return Objects.hash(id);
	}
}
