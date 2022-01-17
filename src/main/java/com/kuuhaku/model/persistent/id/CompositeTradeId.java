/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

public class CompositeTradeId implements Serializable {
	private int id;
	private String uid;

	public CompositeTradeId() {
	}

	public CompositeTradeId(int id, String uid) {
		this.id = id;
		this.uid = uid;
	}

	public int getId() {
		return id;
	}

	public String getUid() {
		return uid;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CompositeTradeId that = (CompositeTradeId) o;
		return id == that.id && Objects.equals(uid, that.uid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, uid);
	}
}
