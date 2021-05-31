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

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "paidrole")
public class PaidRole {
	@Id
	private String id;

	@Column(columnDefinition = "INT NOT NULL")
	private int price;

	@Column(columnDefinition = "INT NOT NULL DEFAULT -1")
	private long duration;

	@ElementCollection(fetch = FetchType.LAZY)
	@JoinColumn(name = "paidrole_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Map<String, Long> users = new HashMap<>();

	public PaidRole() {
	}

	public PaidRole(String id, int price, long duration) {
		this.id = id;
		this.price = price;
		this.duration = duration;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		this.price = price;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long expiration) {
		this.duration = expiration;
	}

	public Map<String, Long> getUsers() {
		return users;
	}

	public Set<String> getExpiredUsers() {
		if (duration == -1) return Set.of();
		return users.entrySet().stream()
				.filter(e -> e.getValue() <= System.currentTimeMillis())
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
	}

	public void setUsers(HashMap<String, Long> users) {
		this.users = users;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PaidRole paidRole = (PaidRole) o;
		return Objects.equals(id, paidRole.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
