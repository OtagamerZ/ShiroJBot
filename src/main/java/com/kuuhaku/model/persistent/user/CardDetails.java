/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2024  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent.user;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.util.Calc;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Table(name = "card_details", schema = "kawaipon")
public class CardDetails extends DAO<CardDetails> {
	@Id
	@Column(name = "card_uuid", length = 36)
	private String uuid;

	@Column(name = "chrome", nullable = false)
	private boolean chrome;

	@Column(name = "quality", nullable = false)
	private double quality = rollQuality();

	public CardDetails() {
	}

	public CardDetails(String uuid, boolean chrome) {
		this.uuid = uuid;
		this.chrome = chrome;
	}

	public String getUuid() {
		return uuid;
	}

	public boolean isChrome() {
		return chrome;
	}

	public void setChrome(boolean chrome) {
		this.chrome = chrome;
	}

	public double getQuality() {
		return quality;
	}

	public void setQuality(double quality) {
		this.quality = quality;
	}

	public double rollQuality() {
		return Calc.round(Math.max(0, Math.pow(ThreadLocalRandom.current().nextDouble(), 5) * 40 - 20), 1);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CardDetails that = (CardDetails) o;
		return Objects.equals(uuid, that.uuid);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(uuid);
	}
}
