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

import com.kuuhaku.Constants;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.common.dunhun.GearModifiers;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.AffixType;
import com.kuuhaku.util.Utils;
import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "gear", schema = "dunhun")
public class Gear extends DAO<Gear> {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private int id;

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "basetype_id")
	@Fetch(FetchMode.JOIN)
	private Basetype basetype;

	@OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "gear_id", referencedColumnName = "id")
	@Fetch(FetchMode.SUBSELECT)
	private Set<GearAffix> affixes = new LinkedHashSet<>();

	@Transient
	private final GearModifiers modifiers = new GearModifiers();

	public int getId() {
		return id;
	}

	public Basetype getBasetype() {
		return basetype;
	}

	public Set<GearAffix> getAffixes() {
		return affixes;
	}

	public String getName(I18N locale) {
		if (affixes.isEmpty()) return basetype.getInfo(locale).getName();
		else if (affixes.size() > 2) return "[RareItem]";

		String template = switch (locale) {
			case EN, UWU_EN -> "%2 %1 %3";
			case PT, UWU_PT -> "%1 %2 %3";
		};

		String pref = "", suff = "";
		for (GearAffix a : affixes) {
			if (a.getAffix().getType() == AffixType.PREFIX) pref = a.getName(locale);
			else suff = a.getName(locale);
		}

		return template.formatted(basetype.getInfo(locale), pref, suff);
	}

	public GearModifiers getModifiers() {
		return modifiers;
	}

	public int getDmg() {
		return (int) (basetype.getStats().attack() * modifiers.getAttackMult());
	}

	public int getDfs() {
		return (int) (basetype.getStats().defense() * modifiers.getDefenseMult());
	}

	public void load(I18N locale, Hero hero) {
		modifiers.reset();
		for (GearAffix ga : affixes) {
			try {
				Affix a = ga.getAffix();
				Utils.exec(getClass().getSimpleName(), a.getEffect(), Map.of(
						"gear", this,
						"hero", hero,
						"values", ga.getValues(locale)
				));
			} catch (Exception e) {
				Constants.LOGGER.warn("Failed to apply affix {}", ga, e);
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Gear gear = (Gear) o;
		return id == gear.id;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
