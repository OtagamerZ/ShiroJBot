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

import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;

import javax.persistence.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;

@Entity
@Table(name = "equipmentmarket")
public class EquipmentMarket {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "market_id_seq")
	private int id;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL")
	private String seller;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String buyer = "";

	@ManyToOne(fetch = FetchType.EAGER)
	private Equipment card;

	@Column(columnDefinition = "INT NOT NULL")
	private int price;

	@Temporal(TemporalType.DATE)
	private Date publishDate = Date.from(Instant.now(Clock.system(ZoneId.of("GMT-3"))));

	public EquipmentMarket(String seller, Equipment card, int price) {
		this.seller = seller;
		this.card = card;
		this.price = price;
	}

	public EquipmentMarket() {
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

	public Equipment getCard() {
		return card;
	}

	public void setCard(Equipment card) {
		this.card = card;
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
