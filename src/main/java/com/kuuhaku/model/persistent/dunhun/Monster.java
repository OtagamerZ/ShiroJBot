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
import com.kuuhaku.model.common.dunhun.Actor;
import com.kuuhaku.model.common.RandomList;
import com.kuuhaku.model.common.dunhun.MonsterBase;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.AffixType;
import com.kuuhaku.model.enums.dunhun.NodeType;
import com.kuuhaku.model.enums.dunhun.RarityClass;
import com.kuuhaku.model.enums.dunhun.Team;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

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

	private transient final Set<Affix> affixes = new LinkedHashSet<>();
	private transient String nameCache;
	private transient int nameHash = 0;

	public Monster() {
	}

	public Monster(String id) {
		super(id);
	}

	@Override
	public String getName(I18N locale) {
		if (nameCache != null) {
			if (nameHash == affixes.hashCode()) {
				return nameCache;
			}
		}

		AtomicReference<String> ending = new AtomicReference<>(Utils.getOr(getInfo(locale).getEnding(), "M"));
		if (affixes.isEmpty()) {
			nameCache = getInfo(locale).getName();
		} else if (getRarityClass() == RarityClass.RARE) {
			String loc = locale.getParent().name().toLowerCase();
			String prefix = IO.getLine("dunhun/monster/prefix/" + loc + ".dict", Calc.rng(0, 32, SERIAL));
			String suffix = IO.getLine("dunhun/monster/suffix/" + loc + ".dict", Calc.rng(0, 32, -SERIAL));

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
					if (parts > 1 && Calc.chance(25, prefix.hashCode() + suffix.hashCode())) {
						part += "'";
					}
				} else {
					part = part.toLowerCase();
				}

				name.append(part);
			}

			nameCache = name + ", " + prefix + " " + suffix;
		} else {
			String template = switch (locale) {
				case EN, UWU_EN -> "%2$s%1$s%3$s";
				case PT, UWU_PT -> "%1$s%2$s%3$s";
			};

			String pref = "", suff = "";
			for (Affix a : affixes) {
				if (a.getType() == AffixType.MON_PREFIX) pref = " " + a.getInfo(locale).getName();
				else suff = " " + a.getInfo(locale).getName();
			}

			nameCache = Utils.regex(template.formatted(getInfo(locale).getName(), pref, suff), "\\[(?<F>[^\\[\\]]*?)\\|(?<M>[^\\[\\]]*?)]")
					.replaceAll(r -> r.group(ending.get()));
		}

		nameHash = affixes.hashCode();
		return nameCache;
	}

	@Override
	public int getMaxHp() {
		int flat = getStats().getBaseHp() + getLevel() * 5;
		double mult = switch (getRarityClass()) {
			case MAGIC -> 1.5;
			case RARE -> 2.25;
			default -> 1;
		} * HP_TABLE[getLevel() - 1];

		if (getGame().getPartySize() > 1 && getTeam() == Team.KEEPERS) {
			mult *= 1 + getGame().getPartySize() * 0.5;
		}

		return (int) Math.max(1, getModifiers().getMaxHp(flat) * mult);
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
	public void load() {
		getModifiers().clear();

		for (Affix a : affixes) {
			a.apply(this);
		}
	}

	@Override
	public Actor<?> copy() {
		Monster clone = new Monster(getId());
		clone.stats = stats;
		clone.infos = infos;
		clone.affixes.addAll(affixes);
		clone.getModifiers().copyFrom(getModifiers());
		clone.getBinding().bind(getBinding());
		clone.setHp(getHp());
		clone.setAp(getAp());

		return clone;
	}

	public static String getRandomId(Dunhun game) {
		List<Object[]> mons = DAO.queryAllUnmapped("SELECT id, weight FROM monster WHERE weight > 0");
		if (mons.isEmpty()) return null;

		RandomList<String> rl = new RandomList<>(game.getNodeRng());
		for (Object[] a : mons) {
			rl.add((String) a[0], ((Number) a[1]).intValue());
		}

		return rl.get();
	}

	public static Monster getRandom(Dunhun game) {
		return getRandom(game, getRandomId(game));
	}

	public static Monster getRandom(Dunhun game, String id) {
		return getRandom(game, id, null);
	}

	public static Monster getRandom(Dunhun game, String id, RarityClass rarity) {
		if (id == null) {
			id = getRandomId(game);
			if (id == null) return null;
		}

		int areaLevel = Integer.MAX_VALUE;
		if (game != null) {
			areaLevel = game.getAreaLevel();
		}

		if (rarity == null) {
			int rarityMult = 1;
			if (game != null && game.getAreaType() == NodeType.DANGER) {
				rarityMult = 2;
			}

			if (Calc.chance(5 * rarityMult)) rarity = RarityClass.RARE;
			else if (Calc.chance(35 * rarityMult)) rarity = RarityClass.MAGIC;
			else rarity = RarityClass.NORMAL;
		}

		Monster mon = DAO.find(Monster.class, id);
		if (Utils.equalsAny(rarity, RarityClass.NORMAL, RarityClass.UNIQUE)) return mon;

		List<AffixType> pool = new ArrayList<>(List.of(AffixType.monsterValues()));

		int min = rarity.getMaxMods() / 2;
		List<AffixType> rolled = Utils.getRandomN(pool, Calc.rng(min, min * 2), min);

		for (AffixType type : rolled) {
			Affix af = Affix.getRandom(mon, type, areaLevel);
			if (af == null) continue;

			mon.getAffixes().add(af);
		}

		if (rarity == RarityClass.RARE && mon.getRarityClass() != RarityClass.RARE) {
			Affix af = Affix.getRandom(mon, Utils.getRandomEntry(pool), areaLevel);
			if (af != null) {
				mon.getAffixes().add(af);
			}
		}

		return mon;
	}
}
