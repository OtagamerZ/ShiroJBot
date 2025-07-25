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

package com.kuuhaku.model.records.id;

import com.kuuhaku.controller.DAO;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public record WarnId(
		@Column(name = "id", nullable = false)
		int id,
		@Column(name = "gid", nullable = false)
		String gid,
		@Column(name = "uid", nullable = false)
		String uid
) implements Serializable {
	public WarnId(String gid, String uid) {
		this(DAO.queryNative(Integer.class, "SELECT nextval('warn_id_seq')"), gid, uid);
		DAO.applyNative(null, "CREATE SEQUENCE IF NOT EXISTS warn_id_seq");
	}

	public WarnId {
		if (uid.isBlank() || gid.isBlank()) throw new IllegalArgumentException("UID and GID cannot be blank");
	}
}