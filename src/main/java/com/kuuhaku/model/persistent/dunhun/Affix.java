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
import com.kuuhaku.model.common.dunhun.context.ActorContext;
import com.kuuhaku.model.common.dunhun.context.GearContext;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.AffixType;
import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.kuuhaku.model.persistent.localized.LocalizedAffix;
import com.kuuhaku.model.records.dunhun.ValueRange;
import com.kuuhaku.util.Utils;
import com.ygimenez.json.JSONArray;
import jakarta.persistence.*;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;
import org.hibernate.type.SqlTypes;
import org.intellij.lang.annotations.Language;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "affix", schema = "dunhun")
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

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "req_tags", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray reqTags = new JSONArray();

	@Column(name = "min_level", nullable = false)
	private int minLevel;

	@Column(name = "weight", nullable = false)
	private int weight;

	@Column(name = "affix_group")
	private String group;

	@Language("Groovy")
	@Column(name = "effect", columnDefinition = "TEXT")
	private String effect;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "ranges", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray ranges = new JSONArray();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "tags", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray tags = new JSONArray();

	public String getId() {
		return id;
	}

	public LocalizedAffix getInfo(I18N locale) {
		return infos.parallelStream()
				.filter(ld -> ld.getLocale().is(locale))
				.map(ld -> ld.setUwu(locale.isUwu()))
				.findAny().orElseThrow();
	}

	public AffixType getType() {
		return type;
	}

	public JSONArray getReqTags() {
		return reqTags;
	}

	public int getMinLevel() {
		return minLevel;
	}

	public String getGroup() {
		return group;
	}

	public int getTier() {
		return DAO.queryNative(Integer.class, """
				SELECT x.tier
				FROM (
				     SELECT id
				          , row_number() OVER (ORDER BY id DESC) AS tier
				     FROM affix
				     WHERE get_affix_family(id) = get_affix_family(?1)
				     ) x
				WHERE x.id = ?1
				""", id
		);
	}

	public String getEffect() {
		return effect;
	}

	public String getModFamily() {
		return Utils.regex(id, "_[IVX]+$").replaceAll("");
	}

	public void apply(Actor<?> actor) {
		if (effect == null || !Utils.equalsAny(type, AffixType.monsterValues())) return;

		try {
			Utils.exec(id, effect, Map.of(
					"ctx", new ActorContext(actor)
			));
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to apply actor modifier {}", id, e);
		}
	}

	public void apply(Gear gear) {
		if (effect == null || !Utils.equalsAny(type, AffixType.itemValues())) return;

		try {
			Utils.exec(id, effect, Map.of(
					"ctx", new GearContext(gear)
			));
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to apply item modifier {}", id, e);
		}
	}

	public List<ValueRange> getRanges() {
		List<ValueRange> out = new ArrayList<>();
		for (Object e : ranges) {
			out.add(ValueRange.parse(String.valueOf(e)));
		}

		return out;
	}

	public JSONArray getTags() {
		return tags;
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

	public static Affix getRandom(Gear gear, AffixType type, int maxMods) {
		Basetype base = gear.getBasetype();

		JSONArray tags = new JSONArray(base.getStats().allTags());
		tags.add(base.getStats().gearType().getSlot().name());

		JSONArray affixes = new JSONArray();
		List<String> groups = new ArrayList<>();

		for (GearAffix ga : gear.getAffixes()) {
			affixes.add(ga.getAffix().getId());
			if (ga.getAffix().getGroup() != null) {
				groups.add(ga.getAffix().getGroup());
			}
		}

		AtomicReference<String> only = new AtomicReference<>();
		gear.hasAffix(a -> {
			String tag = Utils.extract(a, "ONLY_ROLL_(\\w+)", 1);
			if (tag != null) {
				only.set(tag);
				return true;
			}

			return false;
		});

		String tp = type != null ? type.name() : "";
		List<Object[]> affs = new ArrayList<>(
				DAO.queryAllUnmapped("""
				SELECT id
				     , weight
				     , type
				FROM affix
				WHERE ((?1 = '' OR type = ?1) AND type NOT LIKE 'MON\\_%')
				  AND weight > 0
				  AND (min_level <= ?2 OR has(cast(?3 AS JSONB), 'ACCESSORY'))
				  AND req_tags <@ cast(?3 AS JSONB)
				  AND NOT (has(req_tags, 'WEAPON') AND has(cast(?3 AS JSONB), 'OFFHAND'))
				  AND NOT has(get_affix_family(cast(?4 AS JSONB)), get_affix_family(id))
				  AND (affix_group IS NULL OR affix_group NOT IN ?5)
				  AND (cast(?6 AS VARCHAR) IS NULL OR has(tags, ?6))
				""", tp, gear.getReqLevel(), tags.toString(), affixes.toString(), groups, only.get())
		);
		if (affs.isEmpty()) return null;

		if (type == null) {
			int prefs = (int) gear.getModifiers().getPrefixes().apply(maxMods / 2d);
			int suffs = (int) gear.getModifiers().getSuffixes().apply(maxMods / 2d);
			List<AffixType> left = new ArrayList<>();
			left.addAll(Utils.generate(prefs, (_) -> AffixType.PREFIX));
			left.addAll(Utils.generate(suffs, (_) -> AffixType.SUFFIX));

			left.removeIf(a -> affs.stream().noneMatch(o -> o[2].equals(a.name())));

			for (GearAffix ga : gear.getAffixes()) {
				int idx = left.indexOf(ga.getAffix().getType());
				if (idx > -1) {
					left.remove(idx);
				}
			}

			if (left.isEmpty()) return null;

			AffixType chosen = Utils.getRandomEntry(left);
			affs.removeIf(o -> !o[2].equals(chosen.name()));
		}

		RandomList<String> rl = new RandomList<>();
		for (Object[] a : affs) {
			rl.add((String) a[0], ((Number) a[1]).intValue());
		}

		if (rl.entries().isEmpty()) return null;
		return DAO.find(Affix.class, rl.get());
	}

	public static Affix getRandom(Monster monster, AffixType type, int level) {
		JSONArray affixes = new JSONArray();
		List<String> groups = new ArrayList<>();

		for (Affix a : monster.getAffixes()) {
			affixes.add(a.getId());
			if (a.getGroup() != null) {
				groups.add(a.getGroup());
			}
		}

		String tp = type != null ? type.name() : "";
		List<Object[]> affs = DAO.queryAllUnmapped("""
				SELECT id
				     , weight
				     , type
				FROM affix
				WHERE ((?1 = '' OR type = ?1) AND type LIKE 'MON\\_%')
				  AND weight > 0
				  AND min_level <= ?2
				  AND NOT has(get_affix_family(cast(?3 AS JSONB)), get_affix_family(id))
				  AND (affix_group IS NULL OR affix_group NOT IN ?4)
				""", tp, level, affixes.toString(), groups);
		if (affs.isEmpty()) return null;

		if (type == null) {
			List<AffixType> left = new ArrayList<>();
			for (int i = 0; i < 2; i++) {
				left.addAll(List.of(AffixType.monsterValues()));
			}

			left.removeIf(a -> affs.stream().noneMatch(o -> o[2].equals(a.name())));

			for (Affix a : monster.getAffixes()) {
				int idx = left.indexOf(a.getType());
				if (idx > -1) {
					left.remove(idx);
				}
			}

			if (left.isEmpty()) return null;

			AffixType chosen = Utils.getRandomEntry(left);
			affs.removeIf(o -> !o[2].equals(chosen.name()));
		}

		RandomList<String> rl = new RandomList<>();
		for (Object[] a : affs) {
			rl.add((String) a[0], ((Number) a[1]).intValue());
		}

		if (rl.entries().isEmpty()) return null;
		return DAO.find(Affix.class, rl.get());
	}
}
