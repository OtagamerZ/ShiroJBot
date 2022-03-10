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

public class CompositeDebuffId implements Serializable {
	private String debuff;
	private String uid;
	private int hero;

	public CompositeDebuffId() {
	}

	public CompositeDebuffId(String debuff, String uid, int hero) {
		this.debuff = debuff;
		this.uid = uid;
		this.hero = hero;
	}

	public String getDebuff() {
		return debuff;
	}

	public String getUid() {
		return uid;
	}

	public int getHero() {
		return hero;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CompositeDebuffId that = (CompositeDebuffId) o;
		return hero == that.hero && Objects.equals(debuff, that.debuff) && Objects.equals(uid, that.uid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(debuff, uid, hero);
	}
}
