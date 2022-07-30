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

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.Rarity;
import com.kuuhaku.util.Utils;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.*;

@Entity
@Table(name = "title")
public class Title extends DAO<Title> {
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

	@Column(name = "check", columnDefinition = "TEXT")
	private String check;

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

	public boolean check(Account acc) {
		try {
			Object out = Utils.exec(Utils.getOr(check, ""), Map.of("acc", acc));

			return (out instanceof Boolean b) && b;
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to check title " + id, e);
			return false;
		}
	}

	@SuppressWarnings("JpaQlInspection")
	public static List<Title> getAllTitles() {
		return DAO.queryAll(Title.class, """
				SELECT t
				FROM Title t
				ORDER BY CASE t.rarity
					WHEN 'COMMON' THEN 1
					WHEN 'UNCOMMON' THEN 2
					WHEN 'RARE' THEN 3
					WHEN 'ULTRA_RARE' THEN 4
					WHEN 'LEGENDARY' THEN 5
					WHEN 'ULTIMATE' THEN 6
				END DESC
				"""
		);
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
