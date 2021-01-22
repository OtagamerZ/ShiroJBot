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

import com.kuuhaku.model.common.Market;

import javax.persistence.*;
import java.time.ZoneId;
import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;

@Entity
@Table(name = "cardmarket")
public class CardMarket implements Market {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "market_id_seq")
	@SequenceGenerator(name = "market_id_seq", allocationSize = 1, schema = "shiro")
	private int id;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL")
	private String seller;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String buyer = "";

	@ManyToOne(fetch = FetchType.EAGER)
	private Card card;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean foil = false;

	@Column(columnDefinition = "INT NOT NULL")
	private int price;

	@Temporal(TemporalType.TIMESTAMP)
	private Date publishDate = Date.from(Instant.now(Clock.system(ZoneId.of("GMT-3"))));

	public CardMarket(String seller, String buyer, KawaiponCard card, int price) {
		this.seller = seller;
		this.buyer = buyer;
		this.card = card.getCard();
		this.foil = card.isFoil();
		this.price = price;
	}

	public CardMarket(String seller, KawaiponCard card, int price) {
		this.seller = seller;
		this.card = card.getCard();
		this.foil = card.isFoil();
		this.price = price;
	}

	public CardMarket() {
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

	public KawaiponCard getCard() {
		return new KawaiponCard(card, foil);
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

	public Date getPublishDate() {
		return publishDate;
	}

	public void setPublishDate(Date publishDate) {
		this.publishDate = publishDate;
	}
}
