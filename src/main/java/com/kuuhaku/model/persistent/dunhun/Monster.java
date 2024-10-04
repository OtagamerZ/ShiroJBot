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
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.common.dunhun.MonsterModifiers;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.AffixType;
import com.kuuhaku.model.enums.dunhun.RarityClass;
import com.kuuhaku.model.enums.dunhun.Team;
import com.kuuhaku.model.enums.shoukan.FrameSkin;
import com.kuuhaku.model.persistent.localized.LocalizedMonster;
import com.kuuhaku.model.persistent.shoukan.CardAttributes;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "monster", schema = "dunhun")
public class Monster extends DAO<Monster> implements Actor {
	@Transient
	public static final Deck DECK = Utils.with(new Deck(), d -> {
		d.getStyling().setFrame(FrameSkin.GLITCH);
	});

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
	private transient String nameCache;
	private transient List<Skill> skillCache;
	private transient Senshi senshiCache;
	private transient Team team;
	private transient int hp = -1;
	private transient int ap;
	private transient boolean flee;

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

	@Override
	public String getName(I18N locale) {
		if (nameCache != null) return nameCache;

		if (affixes.isEmpty()) return nameCache = getInfo(locale).getName();
		else if (getRarityClass() == RarityClass.RARE) {
			int seed = id.hashCode();

			String loc = locale.getParent().name().toLowerCase();
			String prefix = IO.getLine("dunhun/monster/prefix/" + loc + ".dict", Calc.rng(0, 32, seed + affixes.hashCode()));
			String suffix = IO.getLine("dunhun/monster/suffix/" + loc + ".dict", Calc.rng(0, 32, seed - prefix.hashCode()));

			AtomicReference<String> ending = new AtomicReference<>("M");
			prefix = Utils.regex(prefix, "\\[([FM])]").replaceAll(m -> {
				ending.set(m.group(1));
				return "";
			});

			suffix = Utils.regex(suffix, "\\[(?<F>\\w*)\\|(?<M>\\w*)]")
					.replaceAll(r -> r.group(ending.get()));

			StringBuilder name = new StringBuilder();
			for (int i = 0; i < 2; i++) {
				String part = IO.getLine("dunhun/monster/name_parts.dict", Calc.rng(0, 32, seed >> i));
				if (i == 0) {
					if (Calc.chance(25, prefix.hashCode() + suffix.hashCode())) {
						part = part.charAt(0) + "'" + part.substring(1);
					}
				} else {
					part = part.toLowerCase();
				}

				name.append(part);
			}

			return nameCache = name + ", " + prefix + " " + suffix;
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

		return nameCache = template.formatted(getInfo(locale).getName(), pref, suff);
	}

	@Override
	public int getHp() {
		if (hp == -1) hp = getMaxHp();
		return hp;
	}

	@Override
	public void modHp(int value) {
		hp = Calc.clamp(getHp() + value, 0, getMaxHp());
	}

	@Override
	public int getMaxHp() {
		int hp = (int) ((stats.getBaseHp() + modifiers.getMaxHp()) * modifiers.getHpMult());
		switch (getRarityClass()) {
			case RARE -> hp = (int) (hp * 2.25);
			case MAGIC -> hp = (int) (hp * 1.5);
		}

		return hp;
	}

	@Override
	public int getAp() {
		return ap;
	}

	@Override
	public void modAp(int value) {
		ap = Calc.clamp(ap + value, 0, getMaxAp());
	}

	@Override
	public int getInitiative() {
		return 0;
	}

	@Override
	public boolean hasFleed() {
		return flee;
	}

	@Override
	public void setFleed(boolean flee) {
		this.flee = flee;
	}

	@Override
	public int getMaxAp() {
		return Math.max(1, 1 + getModifiers().getMaxAp());
	}

	public MonsterModifiers getModifiers() {
		return modifiers;
	}

	public Set<Affix> getAffixes() {
		return affixes;
	}

	public RarityClass getRarityClass() {
		int pre = 0, suf = 0;
		for (Affix a : affixes) {
			if (a.getType() == AffixType.MON_PREFIX) pre++;
			else if (a.getType() == AffixType.MON_SUFFIX) suf++;
		}

		if (pre > 1 || suf > 1) return RarityClass.RARE;
		else if (pre + suf > 0) return RarityClass.MAGIC;
		else return RarityClass.NORMAL;
	}

	@Override
	public List<Skill> getSkills() {
		if (skillCache != null) return skillCache;

		return skillCache = DAO.queryAll(Skill.class, "SELECT s FROM Skill s WHERE s.id IN ?1", stats.getSkills());
	}

	@Override
	public Team getTeam() {
		return team;
	}

	@Override
	public void setTeam(Team team) {
		this.team = team;
	}

	@Override
	public Senshi asSenshi(I18N locale) {
		if (senshiCache != null) return senshiCache;

		Senshi s = new Senshi(id, getName(locale), stats.getRace());
		CardAttributes base = s.getBase();

		modifiers.clear();
		for (Affix a : affixes) {
			if (a == null) continue;

			try {
				Utils.exec(a.getId(), a.getEffect(), Map.of(
						"locale", locale,
						"mon", this
				));
			} catch (Exception e) {
				Constants.LOGGER.warn("Failed to apply modifier {}", a.getId(), e);
			}
		}

		double mult = switch (getRarityClass()) {
			case RARE -> 2;
			case MAGIC -> 1.25;
			default -> 1;
		};

		base.setAtk((int) ((stats.getAttack() + modifiers.getAttack()) * modifiers.getAttackMult() * mult));
		base.setDfs((int) ((stats.getDefense() + modifiers.getDefense()) * modifiers.getDefenseMult() * mult));
		base.setDodge(stats.getDodge() + modifiers.getDodge());
		base.setParry(stats.getParry() + modifiers.getParry());

		base.getTags().add("MONSTER");

		return senshiCache = s;
	}

	@Override
	public BufferedImage render(I18N locale) {
		return asSenshi(locale).render(locale, DECK);
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
		RandomList<String> rl = new RandomList<>();
		List<Object[]> mons = DAO.queryAllUnmapped("SELECT id, weight FROM monster WHERE weight > 0");

		for (Object[] a : mons) {
			rl.add((String) a[0], ((Number) a[1]).intValue());
		}

		return getRandom(rl.get());
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
