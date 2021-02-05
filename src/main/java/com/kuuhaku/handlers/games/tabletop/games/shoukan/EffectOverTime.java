/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.handlers.games.tabletop.games.shoukan;

import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.EffectTrigger;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.Objects;

public class EffectOverTime {
	private final long stamp = System.currentTimeMillis();
	private final EffectTrigger trigger;
	private final TriConsumer<Hand, Hand, Shoukan> effect;
	private int turns;

	public EffectOverTime(EffectTrigger trigger, TriConsumer<Hand, Hand, Shoukan> effect) {
		this.trigger = trigger;
		this.effect = effect;
	}

	public EffectTrigger getTrigger() {
		return trigger;
	}

	public TriConsumer<Hand, Hand, Shoukan> getEffect() {
		return effect;
	}

	public int getTurns() {
		return turns;
	}

	public void decreaseTurn() {
		this.turns--;
	}

	public void setTurns(int turns) {
		this.turns = turns;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EffectOverTime that = (EffectOverTime) o;
		return stamp == that.stamp;
	}

	@Override
	public int hashCode() {
		return Objects.hash(stamp);
	}
}
