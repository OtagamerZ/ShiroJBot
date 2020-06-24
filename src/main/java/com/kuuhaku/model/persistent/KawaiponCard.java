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

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "kawaipon_card")
public class KawaiponCard {
	@EmbeddedId
	private KawaiponCardId id;

	@ManyToOne(fetch = FetchType.EAGER)
	@MapsId("kawaipon_id")
	@JoinColumn(name = "kawaipon_id")
	private Kawaipon kawaipon;

	@ManyToOne(fetch = FetchType.EAGER)
	@MapsId("card_id")
	@JoinColumn(name = "card_id")
	private Card card;

	@Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean foil;

	public KawaiponCard(Kawaipon k, Card c, boolean foil) {
		this.card = c;
		this.foil = foil;
	}

	public KawaiponCard() {
	}

	public KawaiponCardId getId() {
		return id;
	}

	public void setId(KawaiponCardId id) {
		this.id = id;
	}

	public Kawaipon getKawaipon() {
		return kawaipon;
	}

	public void setKawaipon(Kawaipon kawaipon) {
		this.kawaipon = kawaipon;
	}

	public Card getCard() {
		return card;
	}

	public void setCard(Card card) {
		this.card = card;
	}

	public boolean isFoil() {
		return foil;
	}

	public void setFoil(boolean foil) {
		this.foil = foil;
	}

	public String getName() {
		return (foil ? "× " : "") + card.getName() + (foil ? " ×" : "");
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		KawaiponCard that = (KawaiponCard) o;
		return foil == that.foil &&
				Objects.equals(card, that.card);
	}

	@Override
	public int hashCode() {
		return Objects.hash(card, foil);
	}
}
