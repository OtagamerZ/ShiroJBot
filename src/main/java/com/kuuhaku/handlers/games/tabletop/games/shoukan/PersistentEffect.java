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
import com.kuuhaku.utils.Helper;

import java.util.Objects;
import java.util.Set;

public class PersistentEffect implements Cloneable {
	private final Drawable card;
	private final String source;
	private final Set<EffectTrigger> triggers;
	private final EffectConsumer effect;
	private final Side target;
	private final boolean debuff;
	private Integer turns;
	private Integer limit;

	public PersistentEffect(Drawable card, String source, EffectConsumer effect, Side target, boolean debuff, Integer turns, Integer limit, EffectTrigger... triggers) {
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
		boolean valid = effect.accept(side, index, (turns != null && turns == 0) || (limit != null && limit == 1));
		if (valid && limit != null && limit > 0) limit--;
	}

	public Side getTarget() {
		return target;
	}

	public boolean isDebuff() {
		return debuff;
	}

	public int getTurns() {
		return Helper.getOr(turns, -1);
	}

	public void decreaseTurn() {
		if (this.turns != null)
			this.turns--;
	}

	public void setTurns(int turns) {
		this.turns = turns;
	}

	public int getLimit() {
		return Helper.getOr(limit, -1);
	}

	public void decreaseLimit() {
		if (this.limit != null)
			this.limit--;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public boolean isExpired() {
		if (limit != null && limit <= 0) return true;
		else return turns != null && turns <= 0;
	}

	@Override
	public PersistentEffect clone() {
		return new PersistentEffect(card.copy(), source, effect, target, debuff, turns, limit, triggers.toArray(EffectTrigger[]::new));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		else if (o == null) return false;
		else if (o instanceof Drawable) return card.equals(o);
		else if (getClass() != o.getClass()) return false;

		PersistentEffect that = (PersistentEffect) o;
		return Objects.equals(card, that.card) && Objects.equals(source, that.source);
	}

	@Override
	public int hashCode() {
		return Objects.hash(card, source);
	}
}
