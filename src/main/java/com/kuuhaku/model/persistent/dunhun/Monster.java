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
import com.kuuhaku.model.common.dunhun.MonsterModifiers;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.AffixType;
import com.kuuhaku.model.persistent.localized.LocalizedMonster;
import com.kuuhaku.model.persistent.shoukan.CardAttributes;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import jakarta.persistence.*;
import org.apache.commons.text.WordUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "monster", schema = "dunhun")
public class Monster extends DAO<Monster> {
	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@Embedded
	private MonsterStats stats;

	@OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "id", referencedColumnName = "id")
	@Fetch(FetchMode.SUBSELECT)
	private Set<LocalizedMonster> infos = new HashSet<>();

	private transient final MonsterModifiers modifiers = new MonsterModifiers();
	private transient final Set<Affix> affixes = new LinkedHashSet<>();
	private transient int roll = Calc.rng(Integer.MAX_VALUE);

	public String getId() {
		return id;
	}

	public MonsterStats getStats() {
		return stats;
	}

	public LocalizedMonster getInfo(I18N locale) {
		return infos.parallelStream()
				.filter(ld -> ld.getLocale().is(locale))
				.findAny().orElseThrow();
	}

	public String getName(I18N locale) {
		if (affixes.isEmpty()) return getInfo(locale).getName();

		if (affixes.size() > 2) {
			String loc = locale.getParent().name().toLowerCase();
			String prefix = IO.getLine("dunhun/monster/prefix/" + loc + ".dict", Calc.rng(0, 32, roll - hashCode()));
			String suffix = IO.getLine("dunhun/monster/suffix/" + loc + ".dict", Calc.rng(0, 32, roll - prefix.hashCode()));

			AtomicReference<String> ending = new AtomicReference<>("M");
			suffix = Utils.regex(suffix, "\\[([FM])]").replaceAll(m -> {
				ending.set(m.group(1));
				return "";
			});

			prefix = Utils.regex(prefix, "\\[(?<F>\\w*)\\|(?<M>\\w*)]")
					.replaceAll(r -> r.group(ending.get()));

			StringBuilder name = new StringBuilder();
			for (int i = 0; i < 2; i++) {
				String part = IO.getLine("dunhun/monster/name_parts.dict", Calc.rng(0, 32, roll - i));
				if (i == 0) {
					if (Calc.chance(25)) {
						part = part.charAt(0) + "'" + WordUtils.capitalizeFully(part.substring(1));
					}
				} else {
					part = part.toLowerCase();
				}

				name.append(part);
			}

			return name + ", " + prefix + " " + suffix;
		}

		String template = switch (locale) {
			case EN, UWU_EN -> "%2$s%1$s%3$s";
			case PT, UWU_PT -> "%1$s%2$s%3$s";
		};

		String pref = "", suff = " ";
		for (Affix a : affixes) {
			if (a.getType() == AffixType.MON_PREFIX) pref = " " + a.getInfo(locale).getName();
			else suff = " " + a.getInfo(locale).getName();
		}

		return template.formatted(getInfo(locale).getName(), pref, suff);
	}

	public MonsterModifiers getModifiers() {
		return modifiers;
	}

	public Set<Affix> getAffixes() {
		return affixes;
	}

	public int getRoll() {
		return roll;
	}

	public Senshi asSenshi() {
		Senshi s = new Senshi(id, stats.getRace());
		CardAttributes base = s.getBase();

		base.setAtk(stats.getAttack());
		base.setDfs(stats.getDefense());
		base.setDodge(stats.getDodge());
		base.setParry(stats.getParry());

		base.getTags().add("MONSTER");

		return s;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Monster monster = (Monster) o;
		return Objects.equals(id, monster.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

	public static Monster getRandom() {
		return getRandom(DAO.queryNative(String.class, "SELECT id FROM monster ORDER BY random()"));
	}

	public static Monster getRandom(String id) {
		Monster mon = DAO.find(Monster.class, id);

		if (Calc.chance(50)) {
			for (AffixType type : AffixType.monsterValues()) {
				if (Calc.chance(50)) {
					Affix af = Affix.getRandom(mon, type);
					if (af == null) continue;

					mon.getAffixes().add(af);
				}
			}
		}

		if (Calc.chance(25)) {
			for (AffixType type : AffixType.monsterValues()) {
				if (Calc.chance(50)) {
					Affix af = Affix.getRandom(mon, type);
					if (af == null) continue;

					mon.getAffixes().add(af);
				}
			}
		}

		return mon;
	}
}
