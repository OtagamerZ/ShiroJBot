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

package com.kuuhaku.handlers.games.tabletop.games.shoukan.states;

import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hand;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;

import java.util.List;
import java.util.stream.Collectors;

public class HandState {
	private final List<Drawable> deque;
	private final List<Drawable> cards;
	private final List<Drawable> destinyDeck;
	private final Hero hero;
	private final int baseHp;
	private final int baseManaPerTurn;
	private final float mitigation;
	private final int maxCards;
	private final int manaPerTurn;
	private final int mana;
	private final int hp;
	private final int prevHp;
	private final int suppressTime;
	private final int lockTime;
	private final int nullTime;

	public HandState(Hand h) {
		this.deque = h.getDeque().stream().map(Drawable::copy).collect(Collectors.toList());
		this.cards = h.getCards().stream().map(Drawable::copy).collect(Collectors.toList());
		this.destinyDeck = h.getDestinyDeck().stream().map(Drawable::copy).collect(Collectors.toList());
		if (h.getHero() != null)
			this.hero = h.getHero().clone();
		else
			this.hero = null;
		this.baseHp = h.getBaseHp();
		this.baseManaPerTurn = h.getBaseManaPerTurn();
		this.mitigation = h.getMitigation();
		this.maxCards = h.getMaxCards();
		this.manaPerTurn = h.getManaPerTurn();
		this.mana = h.getMana();
		this.hp = h.getHp();
		this.prevHp = h.getPrevHp();
		this.suppressTime = h.getSuppressTime();
		this.lockTime = h.getLockTime();
		this.nullTime = h.getNullTime();
	}

	public List<Drawable> getDeque() {
		return deque;
	}

	public List<Drawable> getCards() {
		return cards;
	}

	public List<Drawable> getDestinyDeck() {
		return destinyDeck;
	}

	public Hero getHero() {
		return hero;
	}

	public int getBaseHp() {
		return baseHp;
	}

	public int getBaseManaPerTurn() {
		return baseManaPerTurn;
	}

	public float getMitigation() {
		return mitigation;
	}

	public int getMaxCards() {
		return maxCards;
	}

	public int getManaPerTurn() {
		return manaPerTurn;
	}

	public int getMana() {
		return mana;
	}

	public int getHp() {
		return hp;
	}

	public int getPrevHp() {
		return prevHp;
	}

	public int getSuppressTime() {
		return suppressTime;
	}

	public int getLockTime() {
		return lockTime;
	}

	public int getNullTime() {
		return nullTime;
	}
}
