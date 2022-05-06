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

package com.kuuhaku.model.persistent.user;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.utils.Utils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "stashed_card")
public class StashedCard extends DAO {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private int id;

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "card_id")
	@Fetch(FetchMode.JOIN)
	private Card card;

	@OneToOne(mappedBy = "stashEntry")
	private KawaiponCard kawaiponCard;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private CardType type;

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "kawaipon_uid")
	@Fetch(FetchMode.JOIN)
	private Kawaipon kawaipon;

	public StashedCard() {

	}

	public StashedCard(Kawaipon kawaipon, Card card, CardType type) {
		this.card = card;
		this.type = type;
		this.kawaipon = kawaipon;
	}

	public StashedCard(Kawaipon kawaipon, KawaiponCard card) {
		this.card = card.getCard();
		this.kawaiponCard = card;
		this.type = CardType.KAWAIPON;
		this.kawaipon = kawaipon;
	}

	public int getId() {
		return id;
	}

	public Card getCard() {
		return card;
	}

	public KawaiponCard getKawaiponCard() {
		return kawaiponCard;
	}

	public CardType getType() {
		return type;
	}

	public Kawaipon getKawaipon() {
		return kawaipon;
	}

	public void setKawaipon(Kawaipon kawaipon) {
		this.kawaipon = kawaipon;
	}

	@Override
	public String toString() {
		return Utils.getOr(kawaiponCard, (Object) card).toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		StashedCard that = (StashedCard) o;
		return id == that.id && Objects.equals(card, that.card) && type == that.type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, card, type);
	}
}
