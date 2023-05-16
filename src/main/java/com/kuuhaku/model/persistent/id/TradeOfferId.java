/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent.id;

import com.kuuhaku.controller.DAO;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TradeOfferId implements Serializable {
	@Serial
	private static final long serialVersionUID = -4714845905082735314L;

	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private int id;

	@Column(name = "trade_id", nullable = false)
	private int tradeId;

	public TradeOfferId() {
	}

	public TradeOfferId(int tradeId) {
		DAO.applyNative("CREATE SEQUENCE IF NOT EXISTS trade_offer_id_seq");

		this.id = DAO.queryNative(Integer.class, "SELECT nextval('trade_offer_id_seq')");
		this.tradeId = tradeId;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getTradeId() {
		return tradeId;
	}

	public void setTradeId(int tradeId) {
		this.tradeId = tradeId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TradeOfferId that = (TradeOfferId) o;
		return id == that.id && tradeId == that.tradeId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, tradeId);
	}
}