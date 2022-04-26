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

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "trade_offer")
public class TradeOffer extends DAO {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private int id;

	@Column(name = "uuid", nullable = false, unique = true, length = 36)
	private String uuid;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private CardType type;

	public TradeOffer() {
	}

	public TradeOffer(String uuid, CardType type) {
		this.uuid = uuid;
		this.type = type;
	}

	public int getId() {
		return id;
	}

	public String getUUID() {
		return uuid;
	}

	public CardType getType() {
		return type;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TradeOffer that = (TradeOffer) o;
		return Objects.equals(uuid, that.uuid) && type == that.type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(uuid, type);
	}
}
