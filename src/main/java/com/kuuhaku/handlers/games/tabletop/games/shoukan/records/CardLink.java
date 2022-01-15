package com.kuuhaku.handlers.games.tabletop.games.shoukan.records;

import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.SlotColumn;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.utils.Helper;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public record CardLink(AtomicInteger index, Drawable linked, Drawable self) {
	public int getIndex() {
		return index.get();
	}

	public Champion asChampion() {
		if (linked instanceof Champion c)
			return c;

		throw new ClassCastException("Wrong Drawable type.");
	}

	public Equipment asEquipment() {
		if (linked instanceof Equipment e)
			return e;

		throw new ClassCastException("Wrong Drawable type.");
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
			if (linked == null || (!Helper.between(getIndex(), 0, 5) && getIndex() != -1)) return true;
			else if (isFake()) return false;

			SlotColumn sc = linked.getGame().getSlot(linked.getSide(), getIndex());
			Drawable d;
			if (linked instanceof Champion)
				d = sc.getTop();
			else
				d = sc.getBottom();

			return !linked.equals(d);
		} catch (IndexOutOfBoundsException e) {
			Helper.logger(this.getClass()).error(e + ": [" + getIndex() + ", " + linked.getCard().getId() + "]");
			return true;
		}
	}

	public void sync() {
		if (linked instanceof Champion c) {
			c.link((Equipment) self);
		} else {
			((Equipment) linked).link((Champion) self);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (isFake()) return false;
		if (o == null || getClass() != o.getClass()) return false;
		CardLink cardLink = (CardLink) o;
		return getIndex() == cardLink.getIndex();
	}

	@Override
	public int hashCode() {
		return Objects.hash(index, linked);
	}
}
