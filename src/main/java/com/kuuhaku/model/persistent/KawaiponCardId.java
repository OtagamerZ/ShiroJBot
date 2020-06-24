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

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class KawaiponCardId implements Serializable {
	@Column(name = "kawaipon_id")
	private int kawaipon_id;

	@Column(name = "card_id")
	private String card_id;

	public int getKawaipon() {
		return kawaipon_id;
	}

	public void setKawaipon(int kawaipon) {
		this.kawaipon_id = kawaipon;
	}

	public String getCard() {
		return card_id;
	}

	public void setCard(String card) {
		this.card_id = card;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		KawaiponCardId that = (KawaiponCardId) o;
		return kawaipon_id == that.kawaipon_id &&
				Objects.equals(card_id, that.card_id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(kawaipon_id, card_id);
	}
}
