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

package com.kuuhaku.model.persistent.user;

import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.Rarity;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "title")
public class Title {
	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "id")
	@Fetch(FetchMode.SUBSELECT)
	private Set<LocalizedTitle> infos = new HashSet<>();

	@Enumerated(EnumType.STRING)
	@Column(name = "rarity", nullable = false)
	private Rarity rarity;

	public String getId() {
		return id;
	}

	public LocalizedTitle getInfo(I18N locale) {
		return infos.stream()
				.filter(ld -> ld.getLocale() == locale)
				.findFirst()
				.orElseThrow();
	}

	public Rarity getRarity() {
		return rarity;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Title title = (Title) o;
		return Objects.equals(id, title.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
