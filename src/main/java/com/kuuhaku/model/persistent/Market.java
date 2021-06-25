/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.model.enums.CardType;
import com.kuuhaku.utils.Helper;

import javax.persistence.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "market")
public class Market implements com.kuuhaku.model.common.Market {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String seller;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''")
	private String buyer = "";

	@ManyToOne(fetch = FetchType.EAGER)
	private Card card;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean foil = false;

	@Column(columnDefinition = "INT NOT NULL")
	private int price;

	@Column(columnDefinition = "TIMESTAMP")
	private ZonedDateTime publishDate = ZonedDateTime.now(ZoneId.of("GMT-3"));

	@Enumerated(EnumType.STRING)
	private CardType type;

	public Market(String seller, KawaiponCard card, int price) {
		this.seller = seller;
		this.card = card.getCard();
		this.foil = card.isFoil();
		this.price = price;
		this.type = CardType.KAWAIPON;
	}

	public Market(String seller, Equipment card, int price) {
		this.seller = seller;
		this.card = card.getCard();
		this.foil = false;
		this.price = price;
		this.type = CardType.EVOGEAR;
	}

	public Market(String seller, Field card, int price) {
		this.seller = seller;
		this.card = card.getCard();
		this.foil = false;
		this.price = price;
		this.type = CardType.FIELD;
	}

	public Market() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getSeller() {
		return seller;
	}

	public void setSeller(String seller) {
		this.seller = seller;
	}

	public String getBuyer() {
		return buyer;
	}

	public void setBuyer(String buyer) {
		this.buyer = buyer;
	}

	@SuppressWarnings("unchecked")
	public <T> T getCard() {
		return (T) switch (type) {
			case EVOGEAR -> CardDAO.getEquipment(card);
			case FIELD -> CardDAO.getField(card);
			default -> new KawaiponCard(card, foil);
		};
	}

	@Override
	public Card getRawCard() {
		return card;
	}

	public void setCard(KawaiponCard card) {
		this.card = card.getCard();
		this.foil = card.isFoil();
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		this.price = price;
	}

	public ZonedDateTime getPublishDate() {
		return publishDate;
	}

	public void setPublishDate(ZonedDateTime publishDate) {
		this.publishDate = publishDate;
	}

	public CardType getType() {
		return type;
	}

	public int getPriceLimit() {
		return switch (card.getRarity()) {
			case COMMON, UNCOMMON, RARE, ULTRA_RARE, LEGENDARY, FUSION, ULTIMATE -> card.getRarity().getIndex() * Helper.BASE_CARD_PRICE * 25 * (foil ? 2 : 1);
			case EQUIPMENT -> Helper.BASE_EQUIPMENT_PRICE * 25;
			case FIELD -> Helper.BASE_FIELD_PRICE;
		};
	}
}
