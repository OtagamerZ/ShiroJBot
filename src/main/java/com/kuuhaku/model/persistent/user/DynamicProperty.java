/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.model.persistent.id.DynamicPropertyId;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "dynamic_property")
public class DynamicProperty extends DAO<DynamicProperty> {
	@EmbeddedId
	private DynamicPropertyId id;

	@Column(name = "value", nullable = false, columnDefinition = "TEXT")
	private String value;

	@ManyToOne(optional = false)
	@JoinColumn(name = "uid", nullable = false)
	@Fetch(FetchMode.JOIN)
	@MapsId("uid")
	private Account account;

	public DynamicProperty() {
	}

	public DynamicProperty(Account account, String id, Object value) {
		this.id = new DynamicPropertyId(account.getUid(), id);
		this.value = String.valueOf(value);
		this.account = account;
	}

	public DynamicPropertyId getId() {
		return id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = String.valueOf(value);
	}

	public Account getAccount() {
		return account;
	}

	public static String get(String uid, String key, String defaultValue) {
		DynamicProperty dp = DAO.find(DynamicProperty.class, new DynamicPropertyId(uid, key));
		if (dp == null) return defaultValue;

		return dp.getValue();
	}

	public static void update(String uid, String key, Object value) {
		DAO.applyNative("""
				INSERT INTO dynamic_property (id, uid, value)
				VALUES (?1, ?2, ?3)
				ON CONFLICT (id, uid) DO UPDATE
				SET value = ?3
				""", key, uid, value);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DynamicProperty that = (DynamicProperty) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
