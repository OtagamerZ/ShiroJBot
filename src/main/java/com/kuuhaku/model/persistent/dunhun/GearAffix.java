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
import com.kuuhaku.model.common.dunhun.AffixModifiers;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.RarityClass;
import com.kuuhaku.model.records.id.GearAffixId;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import jakarta.persistence.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "gear_affix", schema = "dunhun")
public class GearAffix extends DAO<GearAffix> {
	@EmbeddedId
	private GearAffixId id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "gear_id", nullable = false, updatable = false)
	@Fetch(FetchMode.JOIN)
	@MapsId("gearId")
	private Gear gear;

	@ManyToOne(optional = false)
	@JoinColumn(name = "affix_id", nullable = false, updatable = false)
	@Fetch(FetchMode.JOIN)
	@MapsId("affixId")
	private Affix affix;

	@Column(name = "roll", nullable = false)
	private int roll = Calc.rng(Integer.MAX_VALUE);

	private transient final AffixModifiers modifiers = new AffixModifiers();

	public GearAffix() {
	}

	public GearAffix(Gear gear, Affix affix) {
		this(gear, affix, Calc.rng(Integer.MAX_VALUE));
	}

	public GearAffix(Gear gear, Affix affix, int roll) {
		this.id = new GearAffixId(gear.getId(), affix.getId());
		this.gear = gear;
		this.affix = affix;
		this.roll = roll;
	}

	public GearAffixId getId() {
		return id;
	}

	public Gear getGear() {
		return gear;
	}

	public Affix getAffix() {
		return affix;
	}

	public int getRoll() {
		return roll;
	}

	public void reroll() {
		this.roll = Calc.rng(Integer.MAX_VALUE);
	}

	public boolean isLocked() {
		return switch (affix.getType()) {
			case PREFIX, MON_PREFIX -> gear.hasAffix("PREFIX_LOCK");
			case SUFFIX, MON_SUFFIX -> gear.hasAffix("SUFFIX_LOCK");
		};
	}

	public String getName(I18N locale) {
		String ending = Utils.getOr(gear.getBasetype().getInfo(locale).getEnding(), "M");

		return Utils.regex(affix.getInfo(locale).getName(), "\\[(?<F>[^\\[\\]]*?)\\|(?<M>[^\\[\\]]*?)]")
				.replaceAll(r -> r.group(ending));
	}

	public String getDescription(I18N locale) {
		return getDescription(locale, false);
	}

	public String getDescription(I18N locale, boolean showScaling) {
		List<Integer> vals = getValues(locale);
		double mult = gear.getRarityClass() == RarityClass.MAGIC ? 1.2 : 1;

		AtomicInteger i = new AtomicInteger();
		return Utils.regex(affix.getInfo(locale).getDescription(), "\\[(-?\\d+)(?:-(-?\\d+))?](%)?")
				.replaceAll(r -> {
					String out;
					if (r.group(3) != null) {
						out = vals.get(i.getAndIncrement()) + "%";
					} else {
						out = Utils.sign(vals.get(i.getAndIncrement()));
					}

					if (showScaling) {
						int min = (int) (Integer.parseInt(r.group(1)) * mult * modifiers.getMinMult());
						int max = (int) (Integer.parseInt(Utils.getOr(r.group(2), r.group(1))) * mult * modifiers.getMaxMult());

						if (min != max) {
							out += " (" + min + " - " + max + ")";
						}
					}

					return out;
				});
	}

	public List<Integer> getValues(I18N locale) {
		List<Integer> values = new ArrayList<>();
		String desc = affix.getInfo(locale).getDescription();
		double mult = gear.getRarityClass() == RarityClass.MAGIC ? 1.2 : 1;

		Matcher m = Utils.regex(desc, "\\[(-?\\d+)(?:-(-?\\d+))?]");
		while (m.find()) {
			int min = (int) (Integer.parseInt(m.group(1)) * mult * modifiers.getMinMult());
			int max = (int) (Integer.parseInt(Utils.getOr(m.group(2), m.group(1))) * mult * modifiers.getMaxMult());

			if (min == max) {
				values.add(min);
				continue;
			}

			values.add(Calc.rng(min, max, roll + desc.hashCode()));
		}

		return values;
	}

	public AffixModifiers getModifiers() {
		return modifiers;
	}

	public void apply(I18N locale, Gear target, Hero owner) {
		if (affix.getEffect() == null) return;

		try {
			owner.asSenshi(locale);

			Utils.exec(affix.getId(), affix.getEffect(), Map.of(
					"locale", locale,
					"gear", target,
					"actor", owner,
					"values", getValues(locale),
					"grant", Utils.getOr(Utils.extract(getDescription(locale), "\"(.+?)\"", 1), "")
			));
		} catch (Exception e) {
			Constants.LOGGER.warn("Failed to apply modifier {}", affix.getId(), e);
		}
	}

	@Override
	public String toString() {
		return affix.getId();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		GearAffix gearAffix = (GearAffix) o;
		return Objects.equals(id, gearAffix.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
