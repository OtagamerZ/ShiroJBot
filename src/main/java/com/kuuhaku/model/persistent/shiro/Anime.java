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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "anime")
public class Anime extends DAO<Anime> {
	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@Column(name = "visible", nullable = false)
	private boolean visible = true;

	public int getCount() {
		return DAO.queryNative(Integer.class, """
				SELECT COUNT(1)
				FROM Card c
				WHERE c.anime_id = ?1
				AND c.rarity IN ('COMMON', 'UNCOMMON', 'RARE', 'ULTRA_RARE', 'LEGENDARY')
				""", id
		);
	}

	@SuppressWarnings("JpaQlInspection")
	public List<Card> getCards() {
		return DAO.queryAll(Card.class, """
				SELECT c
				FROM Card c
				WHERE c.anime.id = ?1
				AND c.rarity IN ('COMMON', 'UNCOMMON', 'RARE', 'ULTRA_RARE', 'LEGENDARY')
				ORDER BY CASE c.rarity
					WHEN 'COMMON' THEN 1
					WHEN 'UNCOMMON' THEN 2
					WHEN 'RARE' THEN 3
					WHEN 'ULTRA_RARE' THEN 4
					WHEN 'LEGENDARY' THEN 5
				END DESC
				""", id
		);
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		if (!visible) return "???";

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
