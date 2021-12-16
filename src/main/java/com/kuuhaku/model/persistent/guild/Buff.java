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

package com.kuuhaku.model.persistent.guild;

import com.kuuhaku.model.enums.BuffType;

import javax.persistence.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Entity
@Table(name = "buff")
public class Buff {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Enumerated(EnumType.STRING)
	private BuffType type;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 1")
	private int tier;

	@Column(columnDefinition = "TIMESTAMP")
	private ZonedDateTime acquiredAt = ZonedDateTime.now(ZoneId.of("GMT-3"));

	public Buff() {
	}

	public Buff(BuffType type, int tier) {
		this.type = type;
		this.tier = tier;
	}

	public int getId() {
		return id;
	}

	public BuffType getType() {
		return type;
	}

	public int getTier() {
		return tier;
	}

	public ZonedDateTime getAcquiredAt() {
		return acquiredAt;
	}

	public long getTime() {
		return TimeUnit.MILLISECONDS.convert(switch (tier) {
			case 1 -> 15;
			case 2 -> 11;
			case 3 -> 7;
			default -> throw new IllegalStateException("Unexpected value: " + tier);
		}, TimeUnit.DAYS);
	}

	public int getPrice() {
		return (int) Math.round(type.getBasePrice() * type.getPriceMult());
	}

	public double getMultiplier() {
		return 1 + type.getPowerMult() * tier;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Buff that = (Buff) o;
		return type == that.type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type);
	}
}
