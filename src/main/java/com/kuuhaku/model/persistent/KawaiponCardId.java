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

package com.kuuhaku.model.persistent;

import javax.persistence.Embeddable;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Table(name = "kawaipon_card_id")
public class KawaiponCardId implements Serializable {

	private int kawaipon;
	private String cards;

	public int getKawaipon() {
		return kawaipon;
	}

	public void setKawaipon(int kawaipon) {
		this.kawaipon = kawaipon;
	}

	public String getCard() {
		return cards;
	}

	public void setCard(String cards) {
		this.cards = cards;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		KawaiponCardId that = (KawaiponCardId) o;
		return kawaipon == that.kawaipon &&
				Objects.equals(cards, that.cards);
	}

	@Override
	public int hashCode() {
		return Objects.hash(kawaipon, cards);
	}
}
