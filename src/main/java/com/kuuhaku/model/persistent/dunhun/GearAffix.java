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
import com.kuuhaku.model.common.dunhun.Actor;
import com.kuuhaku.model.common.dunhun.AffixModifiers;
import com.kuuhaku.model.common.dunhun.context.ActorContext;
import com.kuuhaku.model.common.dunhun.context.GearContext;
import com.kuuhaku.model.enums.I18N;
import com.kuuhaku.model.enums.dunhun.AffixType;
import com.kuuhaku.model.enums.dunhun.RarityClass;
import com.kuuhaku.model.records.dunhun.ValueRange;
import com.kuuhaku.model.records.id.GearAffixId;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;
import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.slf4j.helpers.MessageFormatter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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

	public void reroll() {
		this.roll = Calc.rng(Integer.MAX_VALUE);
	}

	public boolean isLocked() {
		return switch (affix.getType()) {
			case PREFIX, MON_PREFIX -> gear.hasAffix("PREFIX_LOCK");
			case SUFFIX, MON_SUFFIX -> gear.hasAffix("SUFFIX_LOCK");
			default -> true;
		};
	}

	public String getName(I18N locale) {
		if (affix.getType() == AffixType.UNIQUE) {
			return null;
		}

		String ending = Utils.getOr(gear.getBasetype().getInfo(locale).getEnding(), "M");

		return Utils.regex(affix.getInfo(locale).getName(), "\\[(?<F>[^\\[\\]]*?)\\|(?<M>[^\\[\\]]*?)]")
				.replaceAll(r -> r.group(ending));
	}

	public String getDescription(I18N locale) {
		return getDescription(locale, false);
	}

	public String getDescription(I18N locale, boolean showScaling) {
		if (!showScaling) {
			return MessageFormatter.basicArrayFormat(
					affix.getInfo(locale).getDescription(),
					getValues().stream()
							.map("**%s**"::formatted)
							.toArray()
			);
		}

		return MessageFormatter.basicArrayFormat(
				affix.getInfo(locale).getDescription(),
				getRanges().stream()
						.map(r -> "**%s (%s)**".formatted(
								r.withRoll(Calc.rng(1d, roll)), r
						))
						.toArray()
		);
	}

	public List<ValueRange> getRanges() {
		double mult = gear.getRarityClass() == RarityClass.MAGIC ? 1.2 : 1;
		return affix.getRanges().stream()
				.map(r -> r.multiply(
						modifiers.getMinMult().multiplier() * mult,
						modifiers.getMaxMult().multiplier() * mult
				))
				.toList();
	}

	public List<Integer> getValues() {
		return getRanges().stream()
				.map(r -> r.withRoll(Calc.rng(1d, roll)))
				.toList();
	}

	public AffixModifiers getModifiers() {
		return modifiers;
	}

	public void apply(Actor<?> owner) {
		if (affix.getEffect() == null) return;

		try {
			if (Utils.equalsAny(affix.getType(), AffixType.monsterValues())) {
				if (owner != null) {
					Utils.exec(affix.getId(), affix.getEffect(), Map.of(
							"ctx", new ActorContext(owner)
					));
				}
			} else {
				Utils.exec(affix.getId(), affix.getEffect(), Map.of(
						"ctx", new GearContext(gear, owner, getValues())
				));
			}
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
