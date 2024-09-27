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
import com.kuuhaku.model.enums.dunhun.GearSlot;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "owner_id")
	@Fetch(FetchMode.JOIN)
	private Hero owner;

	@OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "gear_id", referencedColumnName = "id")
	@Fetch(FetchMode.SUBSELECT)
	private Set<GearAffix> affixes = new LinkedHashSet<>();

	@Column(name = "base_roll", nullable = false)
	private int roll = Calc.rng(Integer.MAX_VALUE);

	private transient final GearModifiers modifiers = new GearModifiers();

	public Gear() {
	}

	public Gear(Hero owner, Basetype basetype) {
		this.basetype = basetype;
		this.owner = owner;
	}

	public int getId() {
		return id;
	}

	public Basetype getBasetype() {
		return basetype;
	}

	public Hero getOwner() {
		return owner;
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

		AtomicInteger seq = new AtomicInteger();
		Pattern pat = Utils.regex("\\[.+?]");
		List<GearAffix> affixes = this.affixes.stream()
				.sorted(Comparator
						.<GearAffix, Boolean>comparing(ga -> ga.getAffix().getType() == AffixType.SUFFIX, Boolean::compareTo)
						.thenComparing(ga -> ga.getAffix().getId())
				)
				.toList();

		for (GearAffix ga : affixes) {
			String desc = ga.getAffix().getInfo(locale).getDescription();
			List<Integer> vals = ga.getValues(locale);

			seq.set(0);
			desc.lines().forEach(l -> {
				String base = pat.matcher(l.replace("%", "%%"))
						.replaceAll(m -> Matcher.quoteReplacement("[%" + seq.incrementAndGet() + "$s]"));

				mods.compute(base, (k, v) -> {
					if (v == null) return new ArrayList<>(vals);

					for (int j = 0; j < Math.min(v.size(), vals.size()); j++) {
						v.set(j, v.get(j) + vals.get(j));
					}

					return v;
				});
			});
		}

		mods.forEach((k, v) -> {
			Integer[] vals = v.toArray(Integer[]::new);
			if (vals.length > 0 && Arrays.stream(vals).allMatch(i -> i == 0)) return;

			out.add(k.formatted((Object[]) vals));
		});

		return out.stream()
				.map(l -> Utils.regex(l, "\\[(.+?)](%)?").replaceAll(m -> {
					if (m.group(2) != null) {
						return m.group(1) + "%";
					}

					return Utils.sign(Integer.parseInt(m.group(1)));
				}))
				.toList();
	}

	public double getRoll() {
		return roll;
	}

	public String getName(I18N locale) {
		if (affixes.isEmpty()) return basetype.getInfo(locale).getName();
		else if (affixes.size() > 2) return "[RareItem]";

		String template = switch (locale) {
			case EN, UWU_EN -> "%2$s%1$s%3$s";
			case PT, UWU_PT -> "%1$s%2$s%3$s";
		};

		String pref = "", suff = " ";
		for (GearAffix a : affixes) {
			if (a.getAffix().getType() == AffixType.PREFIX) pref = " " + a.getName(locale);
			else suff = " " + a.getName(locale);
		}

		return template.formatted(basetype.getInfo(locale), pref, suff);
	}

	public GearModifiers getModifiers() {
		return modifiers;
	}

	public int getDmg() {
		return (int) ((basetype.getStats().attack() + modifiers.getAttack()) * modifiers.getAttackMult());
	}

	public int getDfs() {
		return (int) ((basetype.getStats().defense() + modifiers.getDefense()) * modifiers.getDefenseMult());
	}

	public float getCritical() {
		return Calc.clamp((basetype.getStats().critical() + modifiers.getCritical()) * modifiers.getCriticalMult(), 0, 100);
	}

	public void load(I18N locale, Hero hero, Senshi senshi) {
		modifiers.reset();

		for (GearAffix ga : getAllAffixes()) {
			try {
				Affix a = ga.getAffix();
				Utils.exec(a.getId(), a.getEffect(), Map.of(
						"locale", locale,
						"gear", this,
						"hero", hero,
						"self", senshi,
						"values", ga.getValues(locale),
						"grant", Utils.getOr(Utils.extract(ga.getDescription(locale), "\"(.+?)\"", 1), "")
				));
			} catch (Exception e) {
				Constants.LOGGER.warn("Failed to apply implicit {}", ga, e);
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

	public static Gear getRandom(Hero hero, GearSlot slot) {
		return getRandom(hero, Basetype.getRandom(slot));
	}

	public static Gear getRandom(Hero hero, Basetype base) {
		Gear out = new Gear(hero, base);

		for (AffixType type : AffixType.validValues()) {
			if (Calc.chance(50)) {
				Affix af = Affix.getRandom(out, type);
				if (af == null) continue;

				out.getAffixes().add(new GearAffix(out, af));
			}
		}

		return out;
	}
}
