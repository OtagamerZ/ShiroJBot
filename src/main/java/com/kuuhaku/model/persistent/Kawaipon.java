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
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(name = "kawaipon")
public class Kawaipon {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(191) DEFAULT ''")
	private String uid = "";

	@ManyToMany(fetch = FetchType.EAGER)
	private Set<Card> cards = new TreeSet<>(Comparator.comparing(Card::getName));

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public Set<Card> getCards() {
		return cards;
	}

	public void addCard(Card card) {
		this.cards.add(card);
	}

	public void removeCard(Card card) {
		this.cards.remove(card);
	}
}
