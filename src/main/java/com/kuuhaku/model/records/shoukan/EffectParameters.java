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
import com.kuuhaku.game.Shoukan;
import com.kuuhaku.model.enums.shoukan.Charm;
import com.kuuhaku.model.enums.shoukan.Flag;
import com.kuuhaku.model.enums.shoukan.TargetType;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Senshi;

import java.util.*;

public record EffectParameters(Trigger trigger, Source source, Target... targets) {
	public EffectParameters(Trigger trigger) {
		this(trigger, new Source());
	}

	public EffectParameters(Trigger trigger, Target... targets) {
		this(trigger, new Source(), targets);
	}

	public EffectParameters(Trigger trigger, Source source, Target... targets) {
		this.trigger = trigger;
		this.source = source;

		Set<Target> tgts = new HashSet<>(List.of(targets));
		if (source.card() instanceof Senshi s && s.getStats().hasFlag(Flag.EMPOWERED)) {
			Shoukan game = source.card().getHand().getGame();

			Iterator<Target> it = tgts.iterator();
			while (it.hasNext()) {
				Target tgt = it.next();
				boolean support = tgt.card().isSupporting();

				if (tgt.index() > 0) {
					tgts.add(new Target(
							game.getSlots(tgt.side()).get(tgt.index() - 1).getAtRole(support),
							tgt.side(),
							tgt.index() - 1, tgt.trigger(), tgt.type()
					));
				}
			}
		}

		this.targets = targets;
	}

	public void consumeShields() {
		for (int i = 0; i < targets.length; i++) {
			Target t = targets[i];
			Senshi card = t.card();

			if (card == null || t.type() == TargetType.ALLY) continue;
			if (card.isStasis()) {
				targets[i] = new Target();
				continue;
			}

			Evogear shield = null;
			card.getHand().getGame().trigger(Trigger.ON_EFFECT_TARGET, new Source(card, Trigger.ON_EFFECT_TARGET));
			if (!card.getHand().equals(card.getHand().getGame().getCurrent())) {
				for (Evogear e : card.getEquipments()) {
					if (e.hasCharm(Charm.SHIELD)) {
						shield = e;
					}
				}
			}

			if (shield != null || card.getStats().popFlag(Flag.IGNORE_EFFECT)) {
				if (shield != null) {
					int charges = shield.getStats().getData().getInt("shield", 0) + 1;
					if (charges >= Charm.SHIELD.getValue(shield.getTier())) {
						card.getHand().getGraveyard().add(shield);
					} else {
						shield.getStats().getData().put("shield", charges);
					}
				}

				targets[i] = new Target();
			}
		}
	}

	public int size() {
		int i = targets.length;
		if (source.card() != null) i++;

		return i;
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
}
