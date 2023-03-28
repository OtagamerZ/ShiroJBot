/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.model.persistent.id.CoupleId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
@Table(name = "couple")
public class Couple extends DAO<Couple> {
	@EmbeddedId
	private CoupleId id;

	@Column(name = "created_at", nullable = false)
	private ZonedDateTime createdAt = ZonedDateTime.now(ZoneId.of("GMT-3"));

	public Couple() {
	}

	public Couple(String first, String second) {
		this.id = new CoupleId(first, second);
	}

	public CoupleId getId() {
		return id;
	}

	public ZonedDateTime getCreatedAt() {
		return createdAt;
	}

	public Account getOther(String uid) {
		return DAO.find(Account.class, uid.equals(id.getFirst()) ? id.getSecond() : id.getFirst());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Couple couple = (Couple) o;
		return Objects.equals(id, couple.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
