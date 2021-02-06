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
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.Objects;
import java.util.Set;

public class EffectOverTime {
	private final long stamp = System.currentTimeMillis();
	private final Set<EffectTrigger> triggers;
	private final TriConsumer<Hand, Hand, Pair<Side, Integer>> effect;
	private int turns;

	public EffectOverTime(TriConsumer<Hand, Hand, Pair<Side, Integer>> effect, EffectTrigger... triggers) {
		this.triggers = Set.of(triggers);
		this.effect = effect;
	}

	public Set<EffectTrigger> getTriggers() {
		return triggers;
	}

	public TriConsumer<Hand, Hand, Pair<Side, Integer>> getEffect() {
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
