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

package com.kuuhaku.model.persistent.user;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.persistent.shiro.Card;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.Objects;

@Entity
@Table(name = "market_order")
public class MarketOrder extends DAO<MarketOrder> {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private int id;

	@ManyToOne(optional = false)
	@PrimaryKeyJoinColumn(name = "card_id")
	@Fetch(FetchMode.JOIN)
	private Card card;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@PrimaryKeyJoinColumn(name = "kawaipon_uid")
	@Fetch(FetchMode.JOIN)
	private Kawaipon kawaipon;

	@Column(name = "buyout_price", nullable = false)
	private int buyout = 0;

	public MarketOrder(Kawaipon kawaipon, Card card, int buyout) {
		this.kawaipon = kawaipon;
		this.card = card;
		this.buyout = buyout;
	}

	public int getId() {
		return id;
	}

	public Card getCard() {
		return card;
	}

	public Kawaipon getKawaipon() {
		return kawaipon;
	}

	public int getBuyout() {
		return buyout;
	}

	public StashedCard search() {
		return DAO.query(StashedCard.class, """
				SELECT sc
				FROM StashedCard sc
				WHERE sc.card = ?2
				  AND sc.kawaipon.uid <> ?1
				  AND sc.price IS NOT NULL
				  AND sc.price BETWEEN 1 AND ?3
				ORDER BY sc.price
				""", kawaipon.getUid(), card, buyout);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MarketOrder that = (MarketOrder) o;
		return id == that.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	public static MarketOrder search(StashedCard sc) {
		return DAO.query(MarketOrder.class, """
				SELECT mo
				FROM MarketOrder mo
				WHERE mo.card = ?2
				  AND mo.kawaipon.uid <> ?1
				  AND mo.buyout >= ?3
				ORDER BY mo.buyout DESC
				""", sc.getKawaipon().getUid(), sc.getCard(), sc.getPrice());
	}
}
