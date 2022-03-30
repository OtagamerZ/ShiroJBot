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
import com.kuuhaku.handlers.games.tabletop.games.shoukan.records.CardLink;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.records.Source;

import java.util.List;

public class SlotColumn implements Cloneable {
	private final int index;
	private final Shoukan game;
	private final Hand hand;
	private Champion top = null;
	private Evogear bottom = null;
	private int unavailable = 0;
	private boolean changed = false;

	public SlotColumn(int index, Shoukan game, Hand hand) {
		this.game = game;
		this.index = index;
		this.hand = hand;
	}

	public SlotColumn(int index, Shoukan game, Hand hand, Champion top, Evogear bottom) {
		this.game = game;
		this.index = index;
		this.hand = hand;
		this.top = top;
		this.bottom = bottom;
	}

	public Champion getTop() {
		return top;
	}

	public void setTop(Champion top) {
		if (top != null) {
			top.setIndex(index);
			top.bind(hand);
		}

		Champion curr = this.top;
		this.top = top;
		if (curr != null) {
			if (curr.hasEffect()) {
				curr.getEffect(new EffectParameters(EffectTrigger.FINALIZE, curr.getGame(), curr.getSide(), index, Duelists.of(), curr.getGame().getChannel()));
			}

			if (top == null) {
				for (CardLink link : List.copyOf(curr.getLinkedTo())) {
					curr.unlink(link.asEquipment());
				}
			} else {
				for (CardLink link : List.copyOf(curr.getLinkedTo())) {
					curr.unlink(link.asEquipment());
					top.link(link.asEquipment());
				}
			}
		}

		if (top != null && !top.isFlipped()) {
			game.applyEffect(EffectTrigger.ON_SUMMON, top, hand.getSide(), index, new Source(top, hand.getSide(), index));
		}
	}

	public Evogear getBottom() {
		return bottom;
	}

	public void setBottom(Evogear bottom) {
		if (bottom != null) {
			bottom.setIndex(index);
			bottom.bind(hand);
		}

		Evogear curr = this.bottom;
		this.bottom = bottom;
		if (curr != null) {
			if (curr.hasEffect()) {
				curr.getEffect(new EffectParameters(EffectTrigger.FINALIZE, curr.getGame(), curr.getSide(), index, Duelists.of(), curr.getGame().getChannel()));
			}

			if (curr.getLinkedTo() != null) {
				curr.getLinkedTo().asChampion().unlink(curr);
			}
		}

		if (bottom != null && !bottom.isFlipped()) {
			game.applyEffect(EffectTrigger.ON_SUMMON, bottom, hand.getSide(), index);
		}
	}

	public int getIndex() {
		return index;
	}

	public boolean isUnavailable() {
		if (unavailable > 0) {
			setTop(null);
			setBottom(null);
		}

		return unavailable > 0;
	}

	public int getUnavailableTime() {
		return unavailable;
	}

	public void setUnavailable(int time) {
		this.unavailable = Math.max(0, this.unavailable + time);

		isUnavailable();
	}

	public boolean isChanged() {
		return changed;
	}

	public void setChanged(boolean changed) {
		this.changed = changed;
	}

	public Hand getHand() {
		return hand;
	}

	@Override
	public SlotColumn clone() {
		try {
			SlotColumn sc = (SlotColumn) super.clone();
			if (top != null)
				sc.top = top.deepCopy();
			if (bottom != null)
				sc.bottom = bottom.deepCopy();
			return sc;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}
}
