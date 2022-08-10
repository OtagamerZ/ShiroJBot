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

package com.kuuhaku.model.common.shoukan;

import com.kuuhaku.game.Shoukan;
import com.kuuhaku.model.enums.shoukan.Flag;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.enums.shoukan.Trigger;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.util.Bit;

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
		if (isLocked() || (top != null && !equals(top.getSlot()))) {
			top.executeAssert(Trigger.ON_REMOVE);
			top = null;
		}

		return top;
	}

	public boolean hasTop() {
		return getTop() != null;
	}

	public void setTop(Senshi top) {
		if (top != null && top.getStats().popFlag(Flag.NO_CONVERT)) {
			return;
		}

		if (getTop() != null) {
			this.top.executeAssert(Trigger.ON_REMOVE);
			this.top.setSlot(null);
		}

		this.top = top;
		if (this.top != null) {
			Hand h = game.getHands().get(side);

			this.top.setSlot(this);
			this.top.setHand(h);
			this.top.executeAssert(Trigger.ON_INITIALIZE);

			if (!this.top.isFlipped()) {
				h.getGame().trigger(Trigger.ON_SUMMON, this.top.asSource(Trigger.ON_SUMMON));
			}

			if (h.getOrigin().synergy() == Race.DEMIGOD && this.top.isFusion()) {
				h.modMP(1);
			}
		}
	}

	public Senshi getBottom() {
		if (isLocked() || (bottom != null && !equals(bottom.getSlot()))) {
			bottom.executeAssert(Trigger.ON_REMOVE);
			bottom = null;
		}

		return bottom;
	}

	public boolean hasBottom() {
		return getBottom() != null;
	}

	public void setBottom(Senshi bottom) {
		if (bottom != null && bottom.getStats().popFlag(Flag.NO_CONVERT)) {
			return;
		}

		if (getBottom() != null) {
			this.bottom.executeAssert(Trigger.ON_REMOVE);
			this.bottom.setSlot(null);
		}

		this.bottom = bottom;
		if (this.bottom != null) {
			Hand h = game.getHands().get(side);

			this.bottom.setSlot(this);
			this.bottom.setHand(h);
			this.bottom.getEquipments().clear();
			this.bottom.executeAssert(Trigger.ON_INITIALIZE);

			if (!this.bottom.isFlipped()) {
				h.getGame().trigger(Trigger.ON_SUMMON, this.bottom.asSource(Trigger.ON_SUMMON));
			}

			if (h.getOrigin().synergy() == Race.DEMIGOD && this.bottom.isFusion()) {
				h.modMP(1);
			}
		}
	}

	public List<Senshi> getCards() {
		return Arrays.asList(getTop(), getBottom());
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
