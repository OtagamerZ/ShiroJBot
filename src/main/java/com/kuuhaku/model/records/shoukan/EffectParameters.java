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
import com.kuuhaku.model.enums.shoukan.Flag;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.enums.shoukan.TargetType;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.shoukan.Senshi;

import java.util.*;

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

		Set<Target> tgts = new HashSet<>(List.of(targets));
		if (source.card() instanceof Senshi s) {
			if (s.hasFlag(Flag.EMPOWERED)) {
				for (Target tgt : targets) {
					if (tgt.trigger() == null) continue;

					Senshi t = tgt.card();
					if (t != null) {
						for (Senshi n : t.getNearby()) {
							tgts.add(new Target(n, tgt.trigger(), tgt.type()));
						}
					}
				}
			}

			for (Target tgt : tgts) {
				Senshi card = tgt.card();
				if (card != null) {
					card.setLastInteraction(s);
				}
			}
		}

		this.targets = tgts.toArray(Target[]::new);
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

		return targets;
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

	public boolean isDeferred(Trigger trigger) {
		return referee != null && referee.trigger() == trigger;
	}

	public boolean leeched() {
		return trigger == Trigger.ON_LEECH;
	}

	@Override
	public String toString() {
		return "EffectParameters[" +
				"trigger=" + trigger +
				", side=" + side +
				", source=" + source +
				", targets=" + Arrays.toString(targets) +
				']';
	}
}
