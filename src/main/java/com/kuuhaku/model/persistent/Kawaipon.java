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

import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.controller.postgresql.StashDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.utils.Helper;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "kawaipon")
public class Kawaipon implements Cloneable {
	@Id
	@Column(columnDefinition = "VARCHAR(255)")
	private String uid = "";

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(nullable = false, name = "kawaipon_id")
	private Set<KawaiponCard> cards = new HashSet<>();

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int activeDeck = 0;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(nullable = false, name = "kawaipon_id")
	private List<Deck> decks = new ArrayList<>();

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int activeHero = 0;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(nullable = false, name = "kawaipon_id")
	private List<Hero> heroes = new ArrayList<>();

	public Kawaipon() {
	}

	public Kawaipon(String uid) {
		this.uid = uid;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public KawaiponCard getCard(Card card, boolean foil) {
		for (KawaiponCard kc : cards) {
			if (kc.getCard().equals(card) && kc.isFoil() == foil) return kc;
		}

		return null;
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
			boolean update = false;
			if (decks.size() < acc.getDeckStashCapacity()) {
				for (int i = 0; i < acc.getDeckStashCapacity() - decks.size(); i++) {
					decks.add(new Deck());
					update = true;
				}
			}

			boolean novice = acc.hasNoviceDeck();
			if (decks.stream().noneMatch(Deck::isNovice)) {
				if (novice) {
					decks.add(new Deck(true));
					update = true;
				}
			} else {
				if (!novice) {
					List<Deck> dks = Helper.removeIf(decks, Deck::isNovice);
					if (!dks.isEmpty()) {
						Deck d = dks.get(0);
						for (Equipment e : d.getEquipments()) {
							StashDAO.saveCard(new Stash(uid, e));
						}

						for (Field f : d.getFields()) {
							StashDAO.saveCard(new Stash(uid, f));
						}
					}

					update = true;
				}
			}

			if (update) {
				KawaiponDAO.saveKawaipon(this);
				decks = KawaiponDAO.getKawaipon(uid).getDecks();
			}
		}

		decks.sort(Comparator.comparingInt(Deck::getId));
		return decks;
	}

	public Deck getDeck() {
		List<Deck> dk = getDecks();

		return dk.get(Math.min(activeDeck, dk.size() - 1));
	}

	public void setDeck(int i) {
		this.activeDeck = i;
	}

	public boolean hasCard(Card c, boolean foil) {
		Deck d = getDeck();

		return getCard(c, foil) != null
			   || d.getChampion(c) != null
			   || d.getEquipment(c) != null
			   || d.getField(c) != null;
	}

	public int getActiveHero() {
		return activeHero;
	}

	public List<Hero> getHeroes() {
		heroes.sort(Comparator.comparingInt(Hero::getId));
		return heroes;
	}

	public void setHero(int i) {
		this.activeHero = i;
	}

	public Kawaipon copy() {
		try {
			return (Kawaipon) super.clone();
		} catch (CloneNotSupportedException e) {
			return new Kawaipon();
		}
	}
}
