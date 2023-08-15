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

package com.kuuhaku.model.common.shoukan;

import com.kuuhaku.game.Shoukan;
import com.kuuhaku.model.enums.shoukan.Flag;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.util.Bit;
import com.kuuhaku.util.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SlotColumn {
	private final Shoukan game;
	private final Side side;
	private final int index;

	private Senshi top = null;
	private Senshi bottom = null;
	private byte state = 0;
	/*
	0xF F
      │ └ 0001
      │      └ permanent lock
      └─ (0 - 15) lock time
	 */

	public SlotColumn(Shoukan game, Side side, int index) {
		this.game = game;
		this.side = side;
		this.index = index;
	}

	public Side getSide() {
		return side;
	}

	public int getIndex() {
		return index;
	}

	public Senshi getTop() {
		if (top != null && (isLocked() || !equals(top.getSlot()))) {
			Senshi rem = top;
			top = null;

			rem.executeAssert(Trigger.ON_REMOVE);
			rem.setSlot(null);
		}

		return top;
	}

	public boolean hasTop() {
		return getTop() != null;
	}

	public void setTop(Senshi top) {
		placeCard(top, true, true);
	}

	public Senshi getBottom() {
		if (bottom != null && (isLocked() || !equals(bottom.getSlot()))) {
			Senshi rem = bottom;
			bottom = null;

			rem.executeAssert(Trigger.ON_REMOVE);
			rem.setSlot(null);
		}

		return bottom;
	}

	public boolean hasBottom() {
		return getBottom() != null;
	}

	public void setBottom(Senshi bottom) {
		placeCard(bottom, false, true);
	}

	public Senshi getUnblocked() {
		return Utils.getOr(top, bottom);
	}

	public void setUnblocked(Senshi card) {
		placeCard(card, top != null, false);
	}

	private void placeCard(Senshi card, boolean top, boolean replace) {
		Senshi current = top ? getTop() : getBottom();
		if (Objects.equals(card, current)) return;
		else if (card != null && card.getHand() != null && card.getSide() == side && card.hasFlag(Flag.NO_CONVERT, true)) {
			return;
		}

		if (current != null) {
			current.executeAssert(Trigger.ON_REMOVE);
			current.setSlot(null);
		}

		if (top && (replace || this.top == null)) {
			this.top = card;
		} else if (replace || this.bottom == null) {
			this.bottom = card;
		}

		if (card != null) {
			Hand h = game.getHands().get(side);

			card.setSlot(this);
			card.setHand(h);
			card.executeAssert(Trigger.ON_INITIALIZE);

			if (!card.isFlipped()) {
				h.getGame().trigger(Trigger.ON_SUMMON, card.asSource(Trigger.ON_SUMMON));
			}

			if (h.getOrigin().synergy() == Race.DEMIGOD && card.isFusion()) {
				h.modMP(1);
			}
		}
	}

	public List<Senshi> getCards() {
		return Arrays.asList(getTop(), getBottom());
	}

	public Senshi getAtRole(boolean support) {
		return support ? bottom : top;
	}

	public void replace(Senshi self, Senshi with) {
		if (Objects.equals(self, getTop())) {
			setTop(with);
		} else if (Objects.equals(self, getBottom())) {
			setBottom(with);
		}
	}

	public void swap() {
		Senshi aux = bottom;
		bottom = top;
		top = aux;
	}

	public void swap(Senshi self, Senshi other) {
		if (self == null || other == null) return;

		boolean sup = other.isSupporting();
		SlotColumn sc = other.getSlot();

		other.setSlot(this);
		if (self.isSupporting()) {
			bottom = other;
		} else {
			top = other;
		}

		self.setSlot(sc);
		if (sup) {
			sc.bottom = self;
		} else {
			sc.top = self;
		}
	}

	public int getLock() {
		if (Bit.on(state, 0)) return -1;

		return Bit.get(state, 1, 4);
	}

	public boolean isLocked() {
		return Bit.on(state, 0) || Bit.on(state, 1, 4);
	}

	public void setLock(boolean value) {
		state = (byte) Bit.set(state, 0, value);
	}

	public void setLock(int time) {
		int curr = Bit.get(state, 1, 4);
		state = (byte) Bit.set(state, 1, Math.max(curr, time), 4);
	}

	public void reduceLock(int time) {
		int curr = Bit.get(state, 1, 4);
		state = (byte) Bit.set(state, 1, Math.max(0, curr - time), 4);
	}

	public SlotColumn getLeft() {
		if (index > 0) {
			return game.getArena().getSlots(side).get(index - 1);
		}

		return null;
	}

	public SlotColumn getRight() {
		List<SlotColumn> slts = game.getArena().getSlots(side);
		if (index < slts.size() - 1) {
			return slts.get(index + 1);
		}

		return null;
	}

	public byte getState() {
		return state;
	}

	public void setState(byte state) {
		this.state = state;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SlotColumn that = (SlotColumn) o;
		return side == that.side && index == that.index;
	}

	@Override
	public int hashCode() {
		return Objects.hash(side, index);
	}
}
