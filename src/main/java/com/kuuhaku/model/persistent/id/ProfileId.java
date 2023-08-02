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
public class ProfileId implements Serializable {
	@Serial
	private static final long serialVersionUID = 381070679371267271L;

	@Column(name = "uid", nullable = false)
	private String uid;

	@Column(name = "gid", nullable = false)
	private String gid;

	public ProfileId() {
	}

	public ProfileId(String uid, String gid) {
		this.uid = uid;
		this.gid = gid;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
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
		ProfileId profileId = (ProfileId) o;
		return Objects.equals(uid, profileId.uid) && Objects.equals(gid, profileId.gid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uid, gid);
	}

	@Override
	public String toString() {
		return "UID: " + uid + ", GID: " + gid;
	}
}