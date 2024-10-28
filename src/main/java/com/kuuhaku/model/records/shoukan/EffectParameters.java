/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.records.shoukan;

import com.kuuhaku.exceptions.TargetException;
import com.kuuhaku.interfaces.shoukan.EffectHolder;
import com.kuuhaku.model.common.BondedList;
import com.kuuhaku.model.enums.shoukan.Flag;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.enums.shoukan.TargetType;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.util.Utils;

import java.util.*;
import java.util.stream.Stream;

public record EffectParameters(Trigger trigger, Side side, DeferredTrigger referee, Source source, Target... targets) {
	public EffectParameters(Trigger trigger, Side side) {
		this(trigger, side, new Source());
	}

	public EffectParameters(Trigger trigger, Side side, Target... targets) {
		this(trigger, side, new Source(), targets);
	}

	public EffectParameters(Trigger trigger, Side side, Source source, Target... targets) {
		this(trigger, side, null, source, targets);
	}

	public EffectParameters(Trigger trigger, Side side, DeferredTrigger referee, Source source, Target... targets) {
		this.trigger = trigger;
		this.side = side;
		this.referee = referee;
		this.source = source;
		this.targets = targets;

		if (source.card() instanceof Senshi s) {
			for (Target tgt : targets) {
				Senshi card = tgt.card();
				if (card != null) {
					card.setLastInteraction(s);
				}
			}
		}
	}

	public void consumeShields() {
		if (source.card() instanceof EffectHolder<?> eh && eh.hasFlag(Flag.EMPOWERED)) return;

		for (Target t : targets) {
			Senshi card = t.card();

			if (card != null && card.isProtected(source.card())) {
				t.skip().set(true);
			}
		}
	}

	public int size() {
		int i = targets.length;
		if (source.card() != null) i++;

		return i;
	}

	public boolean isTarget(Senshi card) {
		return Arrays.stream(targets).anyMatch(t -> Objects.equals(t.card(), card));
	}

	public Target[] targets() {
		for (Target t : targets) {
			if (t.card() != null && t.card().getIndex() != t.index()) {
				t.skip().set(true);
			}
		}

		if (source.card() instanceof EffectHolder<?> eh && eh.hasFlag(Flag.EMPOWERED)) {
			return Stream.of(targets)
					.flatMap(t -> Stream.concat(
							Stream.of(t),
							t.card().getNearby().stream().map(n -> n.asTarget(t.trigger(), t.type()))
					))
					.toArray(Target[]::new);
		}

		return targets;
	}

	public Target[] targets(Trigger trigger) {
		if (targets.length == 0) throw new TargetException();

		consumeShields();
		Target[] out = Arrays.stream(targets())
				.filter(t -> !t.skip().get())
				.filter(t -> t.index() > -1 && t.trigger() == trigger)
				.filter(t -> t.card() != null)
				.toArray(Target[]::new);

		if (out.length == 0) throw new TargetException();
		return out;
	}

	public Target[] allies() {
		if (targets.length == 0) throw new TargetException();

		Target[] out = Arrays.stream(targets())
				.filter(t -> !t.skip().get())
				.filter(t -> t.index() > -1 && t.side() == source.side())
				.filter(t -> t.card() != null)
				.toArray(Target[]::new);

		if (out.length == 0) throw new TargetException();
		return out;
	}

	public Target[] enemies() {
		if (targets.length == 0) throw new TargetException();

		consumeShields();
		Target[] out = Arrays.stream(targets())
				.filter(t -> !t.skip().get())
				.filter(t -> t.index() > -1 && t.side() != source.side())
				.filter(t -> t.card() != null)
				.toArray(Target[]::new);

		if (out.length == 0) throw new TargetException();
		return out;
	}

	public Target[] slots(TargetType type) {
		if (targets.length == 0) throw new TargetException();

		Target[] out = Arrays.stream(targets())
				.filter(t -> t.index() > -1 && t.type() == type)
				.toArray(Target[]::new);

		if (out.length == 0) throw new TargetException();
		return out;
	}

	public List<BondedList<Evogear>> equipments(TargetType type) {
		if (targets.length == 0) throw new TargetException();

		List<BondedList<Evogear>> out = Arrays.stream(targets())
				.filter(t -> !t.skip().get())
				.filter(t -> t.index() > -1 && t.type() == type)
				.filter(t -> t.card() != null)
				.map(t -> t.card().getEquipments())
				.filter(e -> !e.isEmpty())
				.toList();

		if (out.isEmpty()) throw new TargetException();
		return out;
	}

	public boolean isDeferred(Trigger... trigger) {
		return isDeferred(List.of(trigger));
	}

	public boolean isDeferred(Collection<Trigger> trigger) {
		return referee != null && Utils.equalsAny(referee.trigger(), trigger);
	}

	public EffectParameters forSide(Side side) {
		return new EffectParameters(trigger, side, referee, source, targets);
	}

	public EffectParameters withTrigger(Trigger trigger) {
		return new EffectParameters(trigger, side, referee, source, targets);
	}
}
