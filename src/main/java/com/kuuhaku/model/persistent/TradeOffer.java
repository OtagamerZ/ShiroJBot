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

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tradeoffer")
public class TradeOffer {
	@Id
	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String uid;

	@LazyCollection(LazyCollectionOption.FALSE)
	@ManyToMany
	private List<Card> content = new ArrayList<>();

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int value = 0;

	public TradeOffer() {
	}

	public TradeOffer(String uid) {
		this.uid = uid;
	}
}
