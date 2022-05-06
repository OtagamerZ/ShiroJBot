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

package com.kuuhaku.model.persistent.id;

import com.kuuhaku.controller.DAO;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class LevelRoleId implements Serializable {
	@Serial
	private static final long serialVersionUID = -7709129291113066206L;

	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private int id;

	@Column(name = "gid", nullable = false)
	private String gid;

	public LevelRoleId() {
	}

	public LevelRoleId(String gid) {
		DAO.applyNative("CREATE SEQUENCE IF NOT EXISTS level_role_id_seq");

		this.id = DAO.queryNative(Integer.class, "SELECT nextval('level_role_id_seq')");
		this.gid = gid;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getGid() {
		return gid;
	}

	public void setGid(String gid) {
		this.gid = gid;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LevelRoleId that = (LevelRoleId) o;
		return id == that.id && Objects.equals(gid, that.gid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, gid);
	}
}