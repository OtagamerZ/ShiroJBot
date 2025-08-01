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

package com.kuuhaku.model.persistent.shiro;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.persistent.user.DynamicProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "global_property", schema = "shiro")
public class GlobalProperty extends DAO<GlobalProperty> {
	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@Column(name = "value", nullable = false, columnDefinition = "TEXT")
	private String value;

	public GlobalProperty() {
	}

	public GlobalProperty(String id, Object value) {
		this.id = id;
		this.value = String.valueOf(value);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = String.valueOf(value);
	}

	public static String get(String key, String defaultValue) {
		GlobalProperty gp = DAO.find(GlobalProperty.class, key);
		if (gp == null) return defaultValue;

		return gp.getValue();
	}

	public static void update(String key, Object value) {
		DAO.applyNative(DynamicProperty.class, """
				INSERT INTO global_property (id, value)
				VALUES (?1, ?2)
				ON CONFLICT (id) DO UPDATE
				SET value = ?2
				""", key, value);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		GlobalProperty that = (GlobalProperty) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
