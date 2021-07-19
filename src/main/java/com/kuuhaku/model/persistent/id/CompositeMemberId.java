/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

import java.io.Serializable;
import java.util.Objects;

public class CompositeMemberId implements Serializable {
	private String uid;
	private String sid;

	public CompositeMemberId() {
	}

	public CompositeMemberId(String uid, String sid) {
		this.uid = uid;
		this.sid = sid;
	}

	public String getUid() {
		return uid;
	}

	public String getSid() {
		return sid;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CompositeMemberId that = (CompositeMemberId) o;
		return Objects.equals(uid, that.uid) && Objects.equals(sid, that.sid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uid, sid);
	}
}
