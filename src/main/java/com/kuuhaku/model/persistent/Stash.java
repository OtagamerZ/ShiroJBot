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

import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Evogear;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.model.enums.CardType;

import javax.persistence.*;

@Entity
@Table(name = "stash")
public class Stash {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String owner;

	@ManyToOne(fetch = FetchType.EAGER)
	private Card card;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean foil = false;

	@Enumerated(EnumType.STRING)
	private CardType type;

	public Stash(String owner, KawaiponCard card) {
		this.owner = owner;
		this.card = card.getCard();
		this.foil = card.isFoil();
		this.type = CardType.KAWAIPON;
	}

	public Stash(String owner, Champion card) {
		this.owner = owner;
		this.card = card.getCard();
		this.foil = false;
		this.type = CardType.KAWAIPON;
	}

	public Stash(String owner, Evogear card) {
		this.owner = owner;
		this.card = card.getCard();
		this.foil = false;
		this.type = CardType.EVOGEAR;
	}

	public Stash(String owner, Field card) {
		this.owner = owner;
		this.card = card.getCard();
		this.foil = false;
		this.type = CardType.FIELD;
	}

	public Stash() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	@SuppressWarnings("unchecked")
	public <T> T getCard() {
		return (T) switch (type) {
			case EVOGEAR -> Evogear.getEvogear(card.getId());
			case FIELD -> Field.getField(card.getId());
			default -> new KawaiponCard(card, foil);
		};
	}

	public Card getRawCard() {
		return card;
	}

	public void setCard(KawaiponCard card) {
		this.card = card.getCard();
		this.foil = card.isFoil();
	}

	public CardType getType() {
		return type;
	}
}
