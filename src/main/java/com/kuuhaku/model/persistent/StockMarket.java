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
@Table(name = "stockmarket")
public class StockMarket {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String uid = "";

	@ManyToOne(fetch = FetchType.EAGER)
	private Card card;

	@Column(columnDefinition = "INT NOT NULL")
	private long investment;

	public StockMarket(String uid, Card card, long investment) {
		this.uid = uid;
		this.card = card;
		this.investment = investment;
	}

	public StockMarket() {
	}

	public int getId() {
		return id;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public Card getCard() {
		return card;
	}

	public void setCard(Card card) {
		this.card = card;
	}

	public long getInvestment() {
		return investment;
	}

	public void setInvestment(long investment) {
		this.investment = investment;
	}
}
