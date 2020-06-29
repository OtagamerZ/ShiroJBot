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
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "kawaipon")
public class Kawaipon {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(191) NOT NULL DEFAULT ''")
	private String uid = "";

	private Set<KawaiponCard> cards = new HashSet<>();

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public KawaiponCard getCard(Card card, boolean foil) {
		return cards.stream().filter(k -> k.getCard().equals(card) && k.isFoil() == foil).findFirst().orElse(null);
	}

	@ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	public Set<KawaiponCard> getCards() {
		return cards;
	}

	public void setCards(Set<KawaiponCard> cards) {
		this.cards = cards;
	}

	public void addCard(KawaiponCard card) {
		this.cards.add(card);
	}

	public void removeCard(KawaiponCard card) {
		this.cards.remove(card);
	}
}
