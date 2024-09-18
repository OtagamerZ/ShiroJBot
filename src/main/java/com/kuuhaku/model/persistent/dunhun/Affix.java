/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2024  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent.dunhun;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.enums.dunhun.AffixType;
import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.intellij.lang.annotations.Language;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "affix")
public class Affix extends DAO<Affix> {
	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "id", referencedColumnName = "id")
	@Fetch(FetchMode.SUBSELECT)
	private Set<LocalizedAffix> infos = new HashSet<>();

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private AffixType type;

	@Column(name = "tier", nullable = false)
	private int tier;

	@Column(name = "weight", nullable = false)
	private int weight;

	@Column(name = "affix_group", nullable = false)
	private int group;

	@Language("Groovy")
	@Column(name = "effect", columnDefinition = "TEXT")
	private String effect;

	public String getId() {
		return id;
	}

	public Set<LocalizedAffix> getInfos() {
		return infos;
	}

	public AffixType getType() {
		return type;
	}

	public int getTier() {
		return tier;
	}

	public int getWeight() {
		return weight;
	}

	public int getGroup() {
		return group;
	}

	public String getEffect() {
		return effect;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Affix affix = (Affix) o;
		return Objects.equals(id, affix.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
