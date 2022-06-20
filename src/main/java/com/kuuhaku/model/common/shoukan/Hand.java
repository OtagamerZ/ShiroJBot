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

package com.kuuhaku.model.common.shoukan;

import com.kuuhaku.Main;
import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.enums.shoukan.Lock;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.persistent.shoukan.Deck;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.shoukan.BaseValues;
import com.kuuhaku.model.records.shoukan.Origin;
import com.kuuhaku.model.records.shoukan.Timed;
import com.kuuhaku.utils.Utils;
import net.dv8tion.jda.api.entities.User;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Hand {
	private final long timestamp = System.currentTimeMillis();

	private final String uid;
	private final Deck userDeck;

	private final Side side;
	private final Origin origin;

	private final List<Drawable> cards = new ArrayList<>();
	private final LinkedList<Drawable> deck = new LinkedList<>();
	private final LinkedList<Drawable> graveyard = new LinkedList<>();
	private final Set<Timed<Lock>> locks = new HashSet<>();

	private final BaseValues base;

	private String name;

	private int hp = 5000;
	private int regen = 0;
	private int mp = 5;

	private transient Account account;

	public Hand(String uid, Side side) {
		this.uid = uid;
		this.userDeck = DAO.find(Account.class, uid).getCurrentDeck();
		this.side = side;
		this.origin = this.userDeck.getOrigins();
		this.base = new BaseValues();
	}

	public String getUid() {
		return uid;
	}

	public User getUser() {
		return Main.getApp().getShiro().getUserById(uid);
	}

	public Deck getUserDeck() {
		return userDeck;
	}

	public Side getSide() {
		return side;
	}

	public Origin getOrigin() {
		return origin;
	}

	public List<Drawable> getCards() {
		return cards;
	}

	public int getHandCount() {
		return (int) cards.stream().filter(Drawable::isSolid).count();
	}

	public LinkedList<Drawable> getDeck() {
		return deck;
	}

	public LinkedList<Drawable> getGraveyard() {
		return graveyard;
	}

	public Set<Timed<Lock>> getLocks() {
		return locks;
	}

	public int getLockTime(Lock lock) {
		return locks.stream()
				.filter(t -> t.obj().equals(lock))
				.map(Timed::time)
				.mapToInt(AtomicInteger::get)
				.findFirst().orElse(0);
	}

	public BaseValues getBase() {
		return base;
	}

	public String getName() {
		if (name == null) {
			name = Utils.getOr(DAO.find(Account.class, uid).getName(), "???");
		}

		return name;
	}

	public int getHp() {
		return hp;
	}

	public void setHp(int hp) {
		this.hp = hp;
	}

	public double getHpPrcnt() {
		return hp / (double) base.hp();
	}

	public int getRegen() {
		return regen;
	}

	public void setRegen(int regen) {
		this.regen = regen;
	}

	public double getRegenPrcnt() {
		return regen / (double) base.hp();
	}

	public int getMp() {
		return mp;
	}

	public void setMp(int mp) {
		this.mp = mp;
	}

	public Account getAccount() {
		if (account == null) {
			account = DAO.find(Account.class, uid);
		}

		return account;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Hand hand = (Hand) o;
		return timestamp == hand.timestamp && Objects.equals(uid, hand.uid) && side == hand.side && Objects.equals(origin, hand.origin);
	}

	@Override
	public int hashCode() {
		return Objects.hash(timestamp, uid, side, origin);
	}
}
