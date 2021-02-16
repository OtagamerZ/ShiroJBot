/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent;

import com.kuuhaku.model.enums.TrophyType;

import javax.persistence.*;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "trophy")
public class Trophy {
	@Id
	private String uid;

	@Enumerated(value = EnumType.STRING)
	@ElementCollection(fetch = FetchType.EAGER)
	private Set<TrophyType> trophies = EnumSet.noneOf(TrophyType.class);

	public Trophy(String uid) {
		this.uid = uid;
	}

	public Trophy() {
	}

	public String getUid() {
		return uid;
	}

	public Set<TrophyType> getTrophies() {
		return trophies;
	}

	public void setTrophies(Set<TrophyType> trophies) {
		this.trophies = trophies;
	}
}
