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

package com.kuuhaku.model.persistent;

import com.kuuhaku.model.enums.CardType;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "tradecard")
public class TradeCard {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@ManyToOne(fetch = FetchType.EAGER)
	private Card card;

	@Enumerated(EnumType.STRING)
	private CardType type = CardType.NONE;

	@Column(columnDefinition = "BOOLEAN NOT NULL")
	private boolean foil = false;

	public TradeCard() {
	}

	public TradeCard(Card card, CardType type) {
		this.card = card;
		this.type = type;
	}

	public TradeCard(Card card, CardType type, boolean foil) {
		this.card = card;
		this.type = type;
		this.foil = foil;
	}

	public int getId() {
		return id;
	}

	public Card getCard() {
		return card;
	}

	public CardType getType() {
		return type;
	}

	public boolean isFoil() {
		return foil;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TradeCard tradeCard = (TradeCard) o;
		return id == tradeCard.id && foil == tradeCard.foil && Objects.equals(card, tradeCard.card) && type == tradeCard.type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, card, type, foil);
	}
}
