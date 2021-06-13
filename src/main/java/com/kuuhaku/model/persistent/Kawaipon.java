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

import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.utils.Helper;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "kawaipon")
public class Kawaipon implements Cloneable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT ''", unique = true)
	private String uid = "";

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "kawaipon_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Set<KawaiponCard> cards = new HashSet<>();

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int activeDeck = 0;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "kawaipon_id")
	@OnDelete(action = OnDeleteAction.CASCADE)
	private List<Deck> decks = new ArrayList<>();

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public KawaiponCard getCard(Card card, boolean foil) {
		return cards.stream().filter(k -> k.getCard().equals(card) && k.isFoil() == foil).findFirst().orElse(null);
	}

	public Set<KawaiponCard> getCards() {
		return cards;
	}

	public void setCards(Set<KawaiponCard> cards) {
		this.cards = cards;
	}

	public void addCards(Set<KawaiponCard> cards) {
		this.cards.addAll(cards);
	}

	public void addCard(KawaiponCard card) {
		this.cards.add(card);
	}

	public void removeCard(KawaiponCard card) {
		this.cards.remove(card);
	}

	public void removeCards(Set<KawaiponCard> cards) {
		this.cards.removeAll(cards);
	}

	public int getActiveDeck() {
		return activeDeck;
	}

	public List<Deck> getDecks() {
		if (uid == null) {
			decks.add(new Deck());
		} else {
			Account acc = AccountDAO.getAccount(uid);
			if (decks.size() < acc.getStashCapacity()) {
				for (int i = 0; i < acc.getStashCapacity() - decks.size(); i++) {
					decks.add(new Deck());
				}
				KawaiponDAO.saveKawaipon(this);
			}
		}

		decks.sort(Comparator.comparingInt(d -> Helper.getOr(d.getId(), 0)));
		return decks;
	}

	public Deck getDeck() {
		if (uid == null) {
			decks.add(new Deck());
		} else {
			Account acc = AccountDAO.getAccount(uid);
			if (decks.size() < acc.getStashCapacity()) {
				for (int i = 0; i < acc.getStashCapacity() - decks.size(); i++) {
					decks.add(new Deck());
				}
				KawaiponDAO.saveKawaipon(this);
			}
		}

		decks.sort(Comparator.comparingInt(d -> Helper.getOr(d.getId(), 0)));
		return decks.get(activeDeck);
	}

	public void setDeck(int i) {
		this.activeDeck = i;
	}

	public Kawaipon copy() {
		try {
			return (Kawaipon) super.clone();
		} catch (CloneNotSupportedException e) {
			return new Kawaipon();
		}
	}
}
