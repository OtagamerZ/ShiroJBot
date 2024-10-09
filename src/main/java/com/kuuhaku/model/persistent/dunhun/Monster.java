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
import com.kuuhaku.game.Dunhun;
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.common.Delta;
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.common.dunhun.ActorModifiers;
import com.kuuhaku.model.common.dunhun.Combat;
import com.kuuhaku.model.common.shoukan.RegDeg;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.AffixType;
import com.kuuhaku.model.enums.dunhun.RarityClass;
import com.kuuhaku.model.enums.dunhun.Team;
import com.kuuhaku.model.enums.shoukan.FrameSkin;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.enums.shoukan.Trigger;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "monster", schema = "dunhun")
public class Monster extends DAO<Monster> implements Actor {
	@Transient
	public static final Deck DECK = Utils.with(new Deck(), d -> {
		d.getStyling().setFrame(FrameSkin.GLITCH);
	});
	@Transient
	public final long SERIAL = ThreadLocalRandom.current().nextLong();

	@Id
	@Column(name = "id", nullable = false)
	private String id;

	@Embedded
	private MonsterStats stats;

	@OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "id", referencedColumnName = "id")
	@Fetch(FetchMode.SUBSELECT)
	private Set<LocalizedMonster> infos = new HashSet<>();

	private transient final ActorModifiers modifiers = new ActorModifiers();
	private transient final RegDeg regDeg = new RegDeg(null);
	private transient final Set<Affix> affixes = new LinkedHashSet<>();
	private transient final Delta<Integer> hp = new Delta<>();
	private transient String nameCache;
	private transient List<Skill> skillCache;
	private transient Senshi senshiCache;
	private transient Dunhun game;
	private transient Team team;
	private transient int ap;
	private transient boolean flee;

	public Monster() {
	}

	public Monster(String id) {
		this.id = id;
	}

	@Override
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
			String loc = locale.getParent().name().toLowerCase();
			String prefix = IO.getLine("dunhun/monster/prefix/" + loc + ".dict", Calc.rng(0, 32, SERIAL + affixes.hashCode()));
			String suffix = IO.getLine("dunhun/monster/suffix/" + loc + ".dict", Calc.rng(0, 32, SERIAL - prefix.hashCode()));

			AtomicReference<String> ending = new AtomicReference<>("M");
			prefix = Utils.regex(prefix, "\\[([FM])]").replaceAll(m -> {
				ending.set(m.group(1));
				return "";
			});

			suffix = Utils.regex(suffix, "\\[(?<F>\\w*)\\|(?<M>\\w*)]")
					.replaceAll(r -> r.group(ending.get()));

			int parts = Calc.rng(1, 3);
			StringBuilder name = new StringBuilder();
			for (int i = 0; i < parts; i++) {
				String part = IO.getLine("dunhun/monster/name_parts.dict", Calc.rng(0, 64, SERIAL >> i));
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

		String pref = "", suff = "";
		for (Affix a : affixes) {
			if (a.getType() == AffixType.MON_PREFIX) pref = " " + a.getInfo(locale).getName();
			else suff = " " + a.getInfo(locale).getName();
		}

		return nameCache = template.formatted(getInfo(locale).getName(), pref, suff);
	}

	@Override
	public Race getRace() {
		return stats.getRace();
	}

	@Override
	public int getHp() {
		if (hp.get() == null) hp.set(getMaxHp());
		return hp.get();
	}

	@Override
	public int getMaxHp() {
		int hp = (int) ((stats.getBaseHp() + modifiers.getMaxHp().get()) * (1 + modifiers.getHpMult().get()));
		double mult = switch (getRarityClass()) {
			case RARE -> 2.25;
			case MAGIC -> 1.5;
			default -> 1;
		} * (1 + game.getTurn() / 5d);

		return (int) (hp * mult);
	}

	@Override
	public int getHpDelta() {
		if (hp.previous() == null) return 0;

		return hp.get() - hp.previous();
	}

	@Override
	public void modHp(int value) {
		if (getHp() == 0) return;

		if (value < 0 && senshiCache != null) {
			value = -value;

			if (senshiCache.isDefending()) {
				value = (int) -Math.max(value / 10f, (2.5 * Math.pow(value, 2)) / (senshiCache.getDfs() + 2.5 * value));
			} else {
				value = (int) -Math.max(value / 5f, (5 * Math.pow(value, 2)) / (senshiCache.getDfs() + 5 * value));
			}

			if (senshiCache.isSleeping()) {
				senshiCache.reduceSleep(999);
			}
		}

		setHp(getHp() + value);

		if (game != null && game.getCombat() != null) {
			Combat comb = game.getCombat();
			if (value < 0) {
				comb.trigger(Trigger.ON_DAMAGE, this);
			} else {
				comb.trigger(Trigger.ON_HEAL, this);
			}
		}
	}

	@Override
	public void setHp(int value) {
		hp.set(Calc.clamp(value, 0, getMaxHp()));
	}

	@Override
	public void revive(int value) {
		if (getHp() > 0) return;

		hp.set(Calc.clamp(value, 0, getMaxHp()));
		if (senshiCache != null) {
			senshiCache.setAvailable(true);
		}
	}

	@Override
	public int getAp() {
		return ap;
	}

	@Override
	public int getMaxAp() {
		return stats.getMaxAp() + (int) Math.max(0, getModifiers().getMaxAp().get());
	}

	@Override
	public void modAp(int value) {
		ap = Calc.clamp(ap + value, 0, getMaxAp());
	}

	@Override
	public int getInitiative() {
		return (int) modifiers.getInitiative().get();
	}

	@Override
	public double getCritical() {
		return (int) (5 * (1 + modifiers.getCritical().get()));
	}

	@Override
	public int getAggroScore() {
		int aggro = 1;
		if (senshiCache != null) {
			aggro = senshiCache.getDmg() / 10 + senshiCache.getDfs() / 20;
		}

		return (int) (aggro * (1 + modifiers.getAggroMult().get()));
	}

	@Override
	public ActorModifiers getModifiers() {
		return modifiers;
	}

	@Override
	public RegDeg getRegDeg() {
		return regDeg;
	}

	@Override
	public boolean hasFleed() {
		return flee;
	}

	@Override
	public void setFleed(boolean flee) {
		this.flee = flee;
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
	public void setGame(Dunhun game) {
		this.game = game;
	}

	@Override
	public Senshi asSenshi(I18N locale) {
		if (senshiCache != null) return senshiCache;

		Senshi s = new Senshi(this, locale);
		CardAttributes base = s.getBase();

		modifiers.clear();
		for (Affix a : affixes) {
			if (a == null) continue;

			try {
				Utils.exec(a.getId(), a.getEffect(), Map.of(
						"locale", locale,
						"actor", this,
						"self", s
				));
			} catch (Exception e) {
				Constants.LOGGER.warn("Failed to apply modifier {}", a.getId(), e);
			}
		}

		double mult = switch (getRarityClass()) {
			case RARE -> 2;
			case MAGIC -> 1.25;
			default -> 1;
		} * (1 + game.getTurn() / 2d);

		base.setAtk((int) (stats.getAttack() * mult));
		base.setDfs((int) (stats.getDefense() * mult));
		base.setDodge(stats.getDodge());
		base.setParry(stats.getParry());

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
		return SERIAL == monster.SERIAL && Objects.equals(id, monster.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(SERIAL, id);
	}

	@Override
	public Actor fork() {
		Monster clone = new Monster(id);
		clone.stats = stats;
		clone.infos = infos;
		clone.skillCache = skillCache;
		clone.team = team;
		clone.game = game;
		clone.affixes.addAll(affixes);

		return clone;
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
