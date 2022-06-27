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

package com.kuuhaku.model.persistent.shiro;

import com.kuuhaku.controller.DAO;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "anime")
public class Anime extends DAO<Anime> {
	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@Column(name = "visible", nullable = false)
	private boolean visible = true;

	@OneToMany(mappedBy = "anime", cascade = CascadeType.ALL, orphanRemoval = true)
	@Fetch(FetchMode.SUBSELECT)
	private Set<Card> cards = new LinkedHashSet<>();

	public Set<Card> getCards() {
		return cards;
	}

	public void setCards(Set<Card> cards) {
		this.cards = cards;
	}

	public String getId() {
		return id;
	}

	public boolean isVisible() {
		return visible;
	}

	@Override
	public String toString() {
		return DAO.queryNative(String.class, "SELECT c.name FROM card c WHERE c.anime_id = ?1 AND c.rarity = 'ULTIMATE'", id);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Anime anime = (Anime) o;
		return Objects.equals(id, anime.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
