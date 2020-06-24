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

@Entity
@Table(name = "kawaipon_card")
@IdClass(KawaiponCardId.class)
public class KawaiponCard {
	@Id
	@ManyToOne
	@JoinColumn(name = "kawaipon_id", referencedColumnName = "id")
	private Kawaipon kawaipon;

	@Id
	@ManyToOne
	@JoinColumn(name = "cards_id", referencedColumnName = "id")
	private Card cards;

	@Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean foil;

	public Kawaipon getKawaipon() {
		return kawaipon;
	}

	public void setKawaipon(Kawaipon kawaipon) {
		this.kawaipon = kawaipon;
	}

	public Card getCard() {
		return cards;
	}

	public void setCard(Card card) {
		this.cards = card;
	}

	public boolean isFoil() {
		return foil;
	}

	public void setFoil(boolean foil) {
		this.foil = foil;
	}
}
