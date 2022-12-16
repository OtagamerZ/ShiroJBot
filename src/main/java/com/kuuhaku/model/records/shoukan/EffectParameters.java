/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.enums.shoukan.Flag;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.enums.shoukan.TargetType;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.shoukan.Senshi;

import java.util.*;

public record EffectParameters(Trigger trigger, Side side, Source source, Target... targets) {
	public EffectParameters(Trigger trigger, Side side) {
		this(trigger, side, new Source());
	}

	public EffectParameters(Trigger trigger, Side side, Target... targets) {
		this(trigger, side, new Source(), targets);
	}

	public EffectParameters(Trigger trigger, Side side, Source source, Target... targets) {
		this.trigger = trigger;
		this.side = side;
		this.source = source;

		Set<Target> tgts = new HashSet<>(List.of(targets));
		if (source.card() instanceof Senshi s) {
			if (s.hasFlag(Flag.EMPOWERED)) {
				for (Target tgt : tgts) {
					if (tgt.trigger() == Trigger.NONE) continue;

					if (tgt.index() > 0) {
						tgts.add(new Target(
								tgt.card().getLeft(),
								tgt.side(),
								tgt.index() - 1,
								tgt.trigger(),
								tgt.type()
						));
					}

					if (tgt.index() < 4) {
						tgts.add(new Target(
								tgt.card().getRight(),
								tgt.side(),
								tgt.index() + 1,
								tgt.trigger(),
								tgt.type()
						));
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
		for (int i = 0; i < targets.length; i++) {
			Target t = targets[i];
			Senshi card = t.card();

			if (card != null && t.type() != TargetType.ALLY && card.isProtected()) {
				targets[i] = new Target();
			}
		}
	}

	public int size() {
		int i = targets.length;
		if (source.card() != null) i++;

		return i;
	}

	public boolean isTarget(Drawable<?> card) {
		return Arrays.stream(targets).anyMatch(t -> Objects.equals(t.card(), card));
	}

	public Target[] allies() {
		Target[] out = Arrays.stream(targets)
				.filter(t -> t.index() > -1 && t.type() == TargetType.ALLY)
				.filter(t -> t.card() != null)
				.toArray(Target[]::new);

		if (out.length == 0) throw new TargetException();
		return out;
	}

	public Target[] enemies() {
		consumeShields();
		Target[] out = Arrays.stream(targets)
				.filter(t -> t.index() > -1 && t.type() == TargetType.ENEMY)
				.filter(t -> t.card() != null)
				.toArray(Target[]::new);

		if (out.length == 0) throw new TargetException();
		return out;
	}

	public Target[] slots(TargetType type) {
		Target[] out = Arrays.stream(targets)
				.filter(t -> t.index() > -1 && t.type() == type)
				.toArray(Target[]::new);

		if (out.length == 0) throw new TargetException();
		return out;
	}

	public boolean deferred() {
		return trigger == Trigger.ON_DEFER;
	}

	public boolean leeched() {
		return trigger == Trigger.ON_LEECH;
	}
}
