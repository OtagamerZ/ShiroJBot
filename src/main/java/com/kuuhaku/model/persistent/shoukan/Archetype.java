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

package com.kuuhaku.model.persistent.shoukan;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.persistent.shiro.Anime;
import com.kuuhaku.util.Utils;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.intellij.lang.annotations.Language;

import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "archetype")
public class Archetype extends DAO<Archetype> {
	@Transient
	public static final Archetype NONE = new Archetype();

	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@OneToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "anime_id")
	@Fetch(FetchMode.JOIN)
	@MapsId("id")
	private Anime anime;

	@Language("Groovy")
	@Column(name = "effect", columnDefinition = "TEXT")
	private String effect;

	public String getId() {
		return id;
	}

	public Anime getAnime() {
		return anime;
	}

	public String getDescription(I18N locale) {
		return LocalizedString.get(locale, "archetype/" + id.toLowerCase(), "");
	}

	public String getEffect() {
		return effect;
	}

	public void execute(Hand hand) {
		if (effect == null) return;

		Utils.exec(effect, Map.of("hand", hand));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Archetype archetype = (Archetype) o;
		return Objects.equals(id, archetype.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
