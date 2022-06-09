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

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.Drawable;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.shoukan.BaseValues;
import com.kuuhaku.model.records.shoukan.Origin;
import com.kuuhaku.utils.Utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Hand {
	private final String uid;
	private final Side side;
	private final Origin origin;

	private final List<Drawable> cards = new ArrayList<>();
	private final LinkedList<Drawable> deck = new LinkedList<>();
	private final LinkedList<Drawable> graveyard = new LinkedList<>();

	private final BaseValues base;

	private String name;

	private int hp = 5000;
	private int mp = 5;

	public Hand(String uid, Side side, Origin origin, BaseValues base) {
		this.uid = uid;
		this.side = side;
		this.origin = origin;
		this.base = base;
	}

	public String getUid() {
		return uid;
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

	public int getMp() {
		return mp;
	}

	public void setMp(int mp) {
		this.mp = mp;
	}
}
