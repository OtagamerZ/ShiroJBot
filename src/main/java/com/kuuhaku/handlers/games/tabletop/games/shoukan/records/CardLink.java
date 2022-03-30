package com.kuuhaku.handlers.games.tabletop.games.shoukan.records;

import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Evogear;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.SlotColumn;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.utils.helpers.MiscHelper;
import com.kuuhaku.utils.helpers.MathHelper;

import java.util.concurrent.atomic.AtomicInteger;

public record CardLink(AtomicInteger index, Drawable linked, Drawable self) {
	public int getIndex() {
		return index.get();
	}

	public Champion asChampion() {
		if (linked instanceof Champion c)
			return c;

		throw new ClassCastException("Wrong Drawable type: " + linked.getClass().getSimpleName() + " " + linked.getCard().getName() + ".");
	}

	public Evogear asEquipment() {
		if (linked instanceof Evogear e)
			return e;

		throw new ClassCastException("Wrong Drawable type: " + linked.getClass().getSimpleName() + " " + linked.getCard().getName() + ".");
	}

	public CardType getType() {
		if (linked instanceof Champion)
			return CardType.SENSHI;
		else
			return CardType.EVOGEAR;
	}

	public boolean isFake() {
		return getIndex() == -1;
	}

	public boolean isInvalid() {
		try {
			if (linked == null || (!MathHelper.between(getIndex(), 0, 5) && getIndex() != -1)) return true;
			else if (linked.getSide() != self.getSide()) return true;
			else if (isFake()) return false;

			SlotColumn sc = linked.getGame().getSlot(linked.getSide(), getIndex());
			Drawable d;
			if (linked instanceof Champion)
				d = sc.getTop();
			else
				d = sc.getBottom();

			return !linked.equals(d);
		} catch (IndexOutOfBoundsException e) {
			MiscHelper.logger(this.getClass()).error(e + ": [" + getIndex() + ", " + linked.getCard().getId() + "]");
			return true;
		}
	}

	public void sync() {
		if (linked instanceof Champion c) {
			c.link((Evogear) self);
		} else {
			((Evogear) linked).link((Champion) self);
		}
	}

	@Override
	public String toString() {
		return index.get() + "-" + linked;
	}
}
