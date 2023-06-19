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

package com.kuuhaku.model.persistent.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DynamicPropertyId implements Serializable {
	@Serial
	private static final long serialVersionUID = 2416077777517157918L;

	@Column(name = "uid", nullable = false)
	private String uid;

	@Column(name = "id", nullable = false)
	private String id;

	public DynamicPropertyId() {
	}

	public DynamicPropertyId(String account, String id) {
		this.uid = account;
		this.id = id;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String account_uid) {
		this.uid = account_uid;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DynamicPropertyId that = (DynamicPropertyId) o;
		return Objects.equals(uid, that.uid) && Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uid, id);
	}
}