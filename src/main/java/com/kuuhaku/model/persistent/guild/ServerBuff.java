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

package com.kuuhaku.model.persistent.guild;

import com.kuuhaku.model.enums.BuffType;

import javax.persistence.*;
import java.util.concurrent.TimeUnit;

@Entity
@Table(name = "serverbuff")
public class ServerBuff {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Enumerated(EnumType.STRING)
	private BuffType type;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 1")
	private int tier;

	@Column(columnDefinition = "BIGINT NOT NULL")
	private long acquiredAt = System.currentTimeMillis();

	public ServerBuff() {
	}

	public ServerBuff(BuffType type, int tier) {
		this.type = type;
		this.tier = tier;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public BuffType getType() {
		return type;
	}

	public void setType(BuffType type) {
		this.type = type;
	}

	public int getTier() {
		return tier;
	}

	public void setTier(int tier) {
		this.tier = tier;
	}

	public long getAcquiredAt() {
		return acquiredAt;
	}

	public void setAcquiredAt(long acquiredAt) {
		this.acquiredAt = acquiredAt;
	}

	public long getTime() {
		return switch (tier) {
			case 1 -> TimeUnit.MILLISECONDS.convert(15, TimeUnit.DAYS);
			case 2 -> TimeUnit.MILLISECONDS.convert(11, TimeUnit.DAYS);
			case 3 -> TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS);
			case 4 -> TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
			default -> throw new IllegalStateException("Unexpected value: " + tier);
		};
	}

	public int getPrice() {
		return switch (type) {
			case XP -> 2500;
			case CARD -> 2000;
			case DROP -> 1400;
			case FOIL -> 4000;
		} * tier * (tier == 4 ? 10 : 1);
	}

	public double getMult() {
		return 1 + switch (type) {
			case XP -> 0.3;
			case CARD, DROP -> 0.2;
			case FOIL -> 0.25;
		};
	}
}
