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
import com.kuuhaku.game.Dunhun;
import com.kuuhaku.interfaces.dunhun.Actor;
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.common.dunhun.MonsterBase;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.AffixType;
import com.kuuhaku.model.enums.dunhun.RarityClass;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Entity
@Table(name = "monster", schema = "dunhun")
public class Monster extends MonsterBase<Monster> {
	@Column(name = "weight", nullable = false)
	private int weight;

	@Transient
	private transient final Set<Affix> affixes = new LinkedHashSet<>();

	@Transient
	private transient String nameCache;

	public Monster() {
	}

	public Monster(String id) {
		super(id);
	}

	@Override
	public String getName(I18N locale) {
		if (nameCache != null) return nameCache;

		AtomicReference<String> ending = new AtomicReference<>(Utils.getOr(getInfo(locale).getEnding(), "M"));
		if (affixes.isEmpty()) return nameCache = getInfo(locale).getName();
		else if (getRarityClass() == RarityClass.RARE) {
			String loc = locale.getParent().name().toLowerCase();
			String prefix = IO.getLine("dunhun/monster/prefix/" + loc + ".dict", Calc.rng(0, 32, SERIAL + affixes.hashCode()));
			String suffix = IO.getLine("dunhun/monster/suffix/" + loc + ".dict", Calc.rng(0, 32, SERIAL - prefix.hashCode()));

			prefix = Utils.regex(prefix, "\\[(?<F>[^\\[\\]]*?)\\|(?<M>[^\\[\\]]*?)]")
					.replaceAll(r -> r.group(ending.get()));

			prefix = Utils.regex(prefix, "\\[([FM])]").replaceAll(m -> {
				ending.set(m.group(1));
				return "";
			});

			suffix = Utils.regex(suffix, "\\[(?<F>[^\\[\\]]*?)\\|(?<M>[^\\[\\]]*?)]")
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

		return nameCache = Utils.regex(template.formatted(getInfo(locale).getName(), pref, suff), "\\[(?<F>[^\\[\\]]*?)\\|(?<M>[^\\[\\]]*?)]")
				.replaceAll(r -> r.group(ending.get()));
	}

	@Override
	public int getMaxHp() {
		double flat = getStats().getBaseHp() + getModifiers().getMaxHp().get() + getGame().getTurn() * 5;
		double mult = switch (getRarityClass()) {
			case RARE -> 2.25;
			case MAGIC -> 1.5;
			default -> 1;
		} * (1 + getGame().getTurn() * 0.15) * (1 + getGame().getDungeon().getAreaLevel() * 0.25);

		return (int) (flat * mult * getModifiers().getHpMult().get());
	}

	@Override
	public int getMaxAp() {
		return Math.max(1, getStats().getMaxAp() + (int) getModifiers().getMaxAp().get() + getGame().getTurn() / 5);
	}

	public Set<Affix> getAffixes() {
		return affixes;
	}

	@Override
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
	protected void load(I18N locale, Senshi s) {
		for (Affix a : affixes) {
			a.apply(locale, this);
		}
	}

	@Override
	public Actor fork() {
		Monster clone = new Monster(getId());
		clone.stats = stats;
		clone.infos = infos;
		clone.skillCache = skillCache;
		clone.setTeam(getTeam());
		clone.setGame(getGame());
		clone.affixes.addAll(affixes);

		return clone;
	}

	public static Monster getRandom(Dunhun game) {
		RandomList<String> rl = new RandomList<>();
		List<Object[]> mons = DAO.queryAllUnmapped("SELECT id, weight FROM monster WHERE weight > 0");

		for (Object[] a : mons) {
			rl.add((String) a[0], ((Number) a[1]).intValue());
		}

		return getRandom(game, rl.get());
	}

	public static Monster getRandom(Dunhun game, String id) {
		return getRandom(game, id, null);
	}

	public static Monster getRandom(Dunhun game, String id, RarityClass rarity) {
		int dropLevel = Integer.MAX_VALUE;
		if (game != null) {
			dropLevel = game.getDungeon().getAreaLevel();
		}

		if (rarity == null) {
			if (Calc.chance(5)) rarity = RarityClass.RARE;
			else if (Calc.chance(35)) rarity = RarityClass.MAGIC;
			else rarity = RarityClass.NORMAL;
		}

		Monster mon = DAO.find(Monster.class, id);
		if (Utils.equalsAny(rarity, RarityClass.NORMAL, RarityClass.UNIQUE)) return mon;

		List<AffixType> pool = new ArrayList<>(List.of(AffixType.monsterValues()));

		int min = rarity == RarityClass.MAGIC ? 1 : 2;
		List<AffixType> rolled = Utils.getRandomN(pool, Calc.rng(min, min * 2), min);

		for (AffixType type : rolled) {
			Affix af = Affix.getRandom(mon, type, dropLevel);
			if (af == null) continue;

			mon.getAffixes().add(af);
		}

		if (rarity == RarityClass.RARE && mon.getRarityClass() != RarityClass.RARE) {
			Affix af = Affix.getRandom(mon, Utils.getRandomEntry(pool), dropLevel);
			if (af != null) {
				mon.getAffixes().add(af);
			}
		}

		return mon;
	}
}
