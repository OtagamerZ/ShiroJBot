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
import com.kuuhaku.model.common.dunhun.Actor;
import com.kuuhaku.model.common.dunhun.EffectBase;
import com.kuuhaku.model.common.dunhun.GearModifiers;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.AffixType;
import com.kuuhaku.model.enums.dunhun.GearSlot;
import com.kuuhaku.model.enums.dunhun.RarityClass;
import com.kuuhaku.model.records.dunhun.Attributes;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.IO;
import com.kuuhaku.util.Utils;
import com.kuuhaku.util.text.Uwuifier;
import com.ygimenez.json.JSONArray;
import jakarta.persistence.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	@ManyToOne
	@PrimaryKeyJoinColumn(name = "unique_id")
	@Fetch(FetchMode.JOIN)
	private Unique unique;

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "owner_id")
	@Fetch(FetchMode.JOIN)
	private Hero owner;

	@OneToMany(mappedBy = "gear", cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@Fetch(FetchMode.SUBSELECT)
	private Set<GearAffix> affixes = new HashSet<>();

	@Column(name = "base_roll", nullable = false)
	private int roll = Calc.rng(Integer.MAX_VALUE);

	@Column(name = "seed", nullable = false)
	private int seed = Calc.rng(Integer.MAX_VALUE);

	@Column(name = "item_level", nullable = false)
	private int itemLevel;

	@Column(name = "immutable", nullable = false)
	private boolean immutable = false;

	private transient final GearModifiers modifiers = new GearModifiers();
	private transient final Set<EffectBase> effects = new HashSet<>();
	private transient Attributes attributes;
	private transient boolean destroyed;

	public Gear() {
	}

	public Gear(Hero owner, Unique unique) {
		this(owner, unique.getBasetype());
		this.unique = unique;
	}

	public Gear(Hero owner, Basetype basetype) {
		this.basetype = basetype;
		this.owner = owner;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Basetype getBasetype() {
		return basetype;
	}

	public int getReqLevel() {
		int minAffix = 0;
		if (getRarityClass() != RarityClass.UNIQUE) {
			minAffix = affixes.parallelStream()
					.mapToInt(ga -> ga.getAffix().getMinLevel())
					.max().orElse(0);
		}

		return Math.max(minAffix, basetype.getStats().requirements().level());
	}

	public boolean isWeapon() {
		return basetype.getStats().gearType().getSlot() == GearSlot.WEAPON && !getTags().contains("OFFHAND");
	}

	public Unique getUnique() {
		return unique;
	}

	public Hero getOwner() {
		return owner;
	}

	public void setOwner(Hero owner) {
		this.owner = owner;
	}

	public List<GearAffix> getAllAffixes() {
		List<GearAffix> affs = new ArrayList<>();
		GearAffix imp = getImplicit();
		if (imp != null) {
			affs.add(imp);
		}
		affs.addAll(affixes);

		return affs;
	}

	public Set<GearAffix> getAffixes() {
		return affixes;
	}

	public boolean hasAffix(String affix) {
		return hasAffix(affix::equalsIgnoreCase);
	}

	public boolean hasAffix(Predicate<String> cond) {
		Affix imp = basetype.getStats().implicit();
		if (imp != null && cond.test(imp.getId())) return true;

		return affixes.parallelStream().anyMatch(ga -> cond.test(ga.getAffix().getId()));
	}

	public RarityClass getRarityClass() {
		if (unique != null) return RarityClass.UNIQUE;

		int pre = 0, suf = 0;
		for (GearAffix ga : affixes) {
			Affix a = ga.getAffix();
			if (a.getType() == AffixType.PREFIX) pre++;
			else if (a.getType() == AffixType.SUFFIX) suf++;
		}

		if (pre > 1 || suf > 1 || pre + suf > 2) return RarityClass.RARE;
		else if (pre + suf > 0) return RarityClass.MAGIC;
		else return RarityClass.NORMAL;
	}

	public void reroll() {
		this.roll = Calc.rng(Integer.MAX_VALUE);
	}

	public GearAffix getImplicit() {
		if (basetype.getStats().implicit() == null) return null;
		return new GearAffix(this, basetype.getStats().implicit(), roll);
	}

	public List<String> getAffixLines(I18N locale) {
		List<String> out = new ArrayList<>();
		LinkedHashMap<String, List<Integer>> mods = new LinkedHashMap<>();

		List<String> affixes = this.affixes.stream()
				.map(g -> g.getDescription(locale, false))
				.flatMap(String::lines)
				.toList();

		Pattern pat = Utils.regex("[+-]\\d+");
		for (String l : affixes) {
			List<Integer> vals = new ArrayList<>();
			String base = pat
					.matcher(l.replace("%", "%%"))
					.replaceAll(m -> {
						vals.add(NumberUtils.toInt(m.group()));
						return Matcher.quoteReplacement("%s");
					});

			mods.compute(base, (k, v) -> {
				if (v == null) return vals;

				for (int j = 0; j < Math.min(v.size(), vals.size()); j++) {
					v.set(j, v.get(j) + vals.get(j));
				}

				return v;
			});
		}

		mods.forEach((k, v) -> {
			Integer[] vals = v.toArray(Integer[]::new);
			if (vals.length > 0 && Arrays.stream(vals).allMatch(i -> i == 0)) return;

			out.add(k.formatted(Arrays.stream(vals).map(Utils::sign).toArray()));
		});

		return out.stream()
				.map(l -> Utils.regex(l, "\\[([^\\[\\]]+?)](%)?").replaceAll(m -> {
					if (m.group(2) != null) {
						return m.group(1) + "%";
					}

					return Utils.sign(NumberUtils.toInt(m.group(1)));
				}))
				.map(s -> locale.isUwu() ? Uwuifier.INSTANCE.uwu(locale, s) : s)
				.toList();
	}

	public double getRoll() {
		return roll;
	}

	public int getSeed() {
		return seed;
	}

	public int getItemLevel() {
		return itemLevel;
	}

	public void setItemLevel(int itemLevel) {
		this.itemLevel = itemLevel;
	}

	public boolean isImmutable() {
		return immutable;
	}

	public void setImmutable(boolean immutable) {
		this.immutable = immutable;
	}

	public String getName(I18N locale) {
		if (unique != null) return unique.getInfo(locale).getName();
		else if (affixes.isEmpty()) return basetype.getInfo(locale).getName();

		if (getRarityClass() == RarityClass.RARE) {
			String loc = locale.getParent().name().toLowerCase();
			String prefix = IO.getLine("dunhun/item/prefix/" + loc + ".dict", Calc.rng(0, 32, seed));
			String suffix = IO.getLine("dunhun/item/suffix/" + loc + ".dict", Calc.rng(0, 32, -seed));

			AtomicReference<String> ending = new AtomicReference<>("M");
			prefix = Utils.regex(prefix, "\\[([FM])]").replaceAll(m -> {
				ending.set(m.group(1));
				return "";
			});

			suffix = Utils.regex(suffix, "\\[(?<F>[^\\[\\]]*?)\\|(?<M>[^\\[\\]]*?)]")
					.replaceAll(r -> r.group(ending.get()));

			return prefix + " " + suffix;
		}

		String template = switch (locale) {
			case EN, UWU_EN -> "%2$s%1$s%3$s";
			case PT, UWU_PT -> "%1$s%2$s%3$s";
		};

		String pref = "", suff = "";
		for (GearAffix a : affixes) {
			if (a.getAffix().getType() == AffixType.PREFIX) pref = " " + a.getName(locale);
			else suff = " " + a.getName(locale);
		}

		return template.formatted(basetype.getInfo(locale).getName(), pref, suff);
	}

	public GearModifiers getModifiers() {
		return modifiers;
	}

	public Set<EffectBase> getEffects() {
		return effects;
	}

	public Attributes getAttributes() {
		if (attributes == null) load(null);

		return attributes;
	}

	public JSONArray getTags() {
		JSONArray out = new JSONArray(basetype.getStats().allTags());
		out.addAll(modifiers.getAddedTags());

		return out;
	}

	public int getDmg() {
		int base = basetype.getStats().attack();

		return (int) (modifiers.getAttack().apply(base));
	}

	public int getDfs() {
		int base = basetype.getStats().defense();

		return (int) (modifiers.getDefense().apply(base));
	}

	public double getCritical() {
		int base = (int) basetype.getStats().critical();

		return Calc.clamp(modifiers.getCritical().apply(base), 0, 100);
	}

	public boolean isDestroyed() {
		return destroyed;
	}

	public void load(Actor<?> owner) {
		attributes = new Attributes();
		modifiers.clear();
		effects.clear();

		Set<GearAffix> meta = new HashSet<>();
		for (GearAffix ga : getAllAffixes()) {
			if (ga.getAffix().getTags().contains("META")) {
				meta.add(ga);
				continue;
			}

			ga.apply(this, owner);
		}

		if (!meta.isEmpty()) {
			for (GearAffix ga : meta) {
				ga.apply(this, owner);
			}
		}
	}

	public Gear copy() {
		Gear clone = new Gear(owner, basetype);
		clone.unique = unique;
		clone.roll = roll;
		clone.seed = seed;

		for (GearAffix affix : affixes) {
			clone.affixes.add(new GearAffix(clone, affix.getAffix(), affix.getRoll()));
		}

		return clone;
	}

	@PostLoad
	private void onPostLoad() {
		load(null);
	}

	@Override
	public void afterDelete() {
		destroyed = true;
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

	public static Gear getRandom(Actor<?> source) {
		return getRandom(source, (RarityClass) null);
	}

	public static Gear getRandom(Actor<?> source, Basetype base) {
		return getRandom(source, base, null);
	}

	public static Gear getRandom(Actor<?> source, RarityClass rarity) {
		return getRandom(source, Basetype.getRandom(source), rarity);
	}

	public static Gear getRandom(Actor<?> source, Basetype base, RarityClass rarity) {
		if (base == null) return null;

		double mult = 1;
		int dropLevel = Actor.MAX_LEVEL;
		if (source != null && source.getGame() != null) {
			dropLevel = source.getDropLevel();

			if (source.getKiller() instanceof Hero h) {
				mult *= h.getModifiers().getMagicFind(1);
			}

			mult *= switch (source.getRarityClass()) {
				case NORMAL -> 1;
				case MAGIC -> 1.2;
				case RARE -> 1.5;
				case UNIQUE -> 2.5;
			};
		}

		if (rarity == null) {
			if (Calc.chance(1 * mult)) rarity = RarityClass.UNIQUE;
			else if (Calc.chance(10 * mult)) rarity = RarityClass.RARE;
			else if (Calc.chance(40 * mult)) rarity = RarityClass.MAGIC;
			else rarity = RarityClass.NORMAL;
		}

		if (rarity == RarityClass.UNIQUE) {
			Unique u = Unique.getRandom(source);
			if (u != null) {
				Gear out = u.asGear();
				out.setItemLevel(dropLevel);

				return out;
			}

			rarity = RarityClass.RARE;
		}

		Gear out = new Gear(null, base);
		out.setItemLevel(dropLevel);

		if (rarity == RarityClass.NORMAL) return out;

		int mods = Calc.rng(1, rarity.getMaxMods());
		for (int i = 0; i < mods; i++) {
			Affix af = Affix.getRandom(out, null, rarity);
			if (af == null) break;

			out.getAffixes().add(new GearAffix(out, af));

			if (i == mods - 1 && mods < rarity.getMaxMods() && out.getRarityClass() != rarity) {
				mods++;
			}
		}

		return out;
	}
}
