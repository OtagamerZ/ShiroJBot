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
	private int hero;
	private String uid;
	private String debuff;

	public CompositeDebuffId() {
	}

	public CompositeDebuffId(int hero, String uid, String debuff) {
		this.hero = hero;
		this.uid = uid;
		this.debuff = debuff;
	}

	public int getHero() {
		return hero;
	}

	public String getUid() {
		return uid;
	}

	public String getDebuff() {
		return debuff;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CompositeDebuffId that = (CompositeDebuffId) o;
		return hero == that.hero && Objects.equals(uid, that.uid) && Objects.equals(debuff, that.debuff);
	}

	@Override
	public int hashCode() {
		return Objects.hash(hero, uid, debuff);
	}
}
