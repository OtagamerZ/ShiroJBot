/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.EffectConsumer;

import java.util.Objects;
import java.util.Set;

public class PersistentEffect {
	private final Drawable card;
	private final String source;
	private final Set<EffectTrigger> triggers;
	private final EffectConsumer effect;
	private final Side target;
	private final boolean debuff;
	private int turns;
	private int limit;

	public PersistentEffect(Drawable card, String source, EffectConsumer effect, Side target, boolean debuff, int turns, int limit, EffectTrigger... triggers) {
		this.card = card;
		this.source = source;
		this.triggers = Set.of(triggers);
		this.effect = effect;
		this.target = target;
		this.turns = turns;
		this.limit = limit;
		this.debuff = debuff;
	}

	public Drawable getCard() {
		return card;
	}

	public String getSource() {
		return source;
	}

	public Set<EffectTrigger> getTriggers() {
		return triggers;
	}

	public EffectConsumer getEffect() {
		return effect;
	}

	public void activate(Side side, int index) {
		if (limit > 0) limit--;
		effect.accept(side, index, turns == 0 || limit == 0);
	}

	public Side getTarget() {
		return target;
	}

	public boolean isDebuff() {
		return debuff;
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

	public int getLimit() {
		return limit;
	}

	public void decreaseLimit() {
		this.limit--;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PersistentEffect that = (PersistentEffect) o;
		return Objects.equals(card, that.card) && Objects.equals(source, that.source);
	}

	@Override
	public int hashCode() {
		return Objects.hash(card, source);
	}
}
