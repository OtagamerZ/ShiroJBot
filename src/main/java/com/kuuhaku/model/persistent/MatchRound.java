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

import com.kuuhaku.controller.postgresql.DrawableDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Evogear;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hand;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.SlotColumn;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.interfaces.Drawable;
import com.kuuhaku.utils.json.JSONArray;

import javax.persistence.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity
@Table(name = "matchround")
public class MatchRound {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Enumerated(value = EnumType.STRING)
	private Side side = Side.BOTTOM;

	@Enumerated(value = EnumType.STRING)
	private Race major = Race.NONE;

	@Enumerated(value = EnumType.STRING)
	private Race minor = Race.NONE;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL")
	private String uid;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 5000")
	private int baseHp = 5000;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 5000")
	private int hp = 5000;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 5")
	private int baseMp = 5;

	@Column(columnDefinition = "INT NOT NULL DEFAULT 0")
	private int mp = 0;

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT '[]'")
	private String champions = "[]";

	@Column(columnDefinition = "VARCHAR(255) NOT NULL DEFAULT '[]'")
	private String evogears = "[]";

	@Column(columnDefinition = "TEXT")
	private String hand = "[]";

	@Column(columnDefinition = "TEXT")
	private String deque = "[]";

	public MatchRound() {
	}

	public void setData(Hand hand, List<SlotColumn> slots) {
		this.side = hand.getSide();
		this.major = hand.getCombo().getLeft();
		this.minor = hand.getCombo().getRight();
		this.uid = hand.getAcc().getUid();
		this.baseHp = hand.getBaseHp();
		this.hp = hand.getHp();
		this.baseMp = hand.getBaseManaPerTurn();
		this.mp = hand.getMana();

		this.champions = Arrays.toString(slots.stream()
				.map(SlotColumn::getTop)
				.filter(Objects::nonNull)
				.map(Drawable::getCard)
				.map(Card::getId)
				.toArray());

		this.evogears = Arrays.toString(slots.stream()
				.map(SlotColumn::getBottom)
				.filter(Objects::nonNull)
				.map(Drawable::getCard)
				.map(Card::getId)
				.toArray());

		this.hand = Arrays.toString(hand.getCards().stream()
				.map(Drawable::getCard)
				.map(Card::getId)
				.toArray());

		this.deque = Arrays.toString(hand.getDeque().stream()
				.map(Drawable::getCard)
				.map(Card::getId)
				.toArray());
	}

	public int getId() {
		return id;
	}

	public Side getSide() {
		return side;
	}

	public Race getMajor() {
		return major;
	}

	public Race getMinor() {
		return minor;
	}

	public String getUid() {
		return uid;
	}

	public int getBaseHp() {
		return baseHp;
	}

	public int getHp() {
		return hp;
	}

	public int getBaseMp() {
		return baseMp;
	}

	public int getMp() {
		return mp;
	}

	public List<Champion> getChampions() {
		return new JSONArray(champions).stream()
				.map(String::valueOf)
				.collect(Collectors.collectingAndThen(Collectors.toList(), Champion::getChampions));
	}


	public List<Evogear> getEvogears() {
		return new JSONArray(evogears).stream()
				.map(String::valueOf)
				.collect(Collectors.collectingAndThen(Collectors.toList(), Evogear::getEvogears));
	}

	public List<Drawable> getHand() {
		return new JSONArray(hand).stream()
				.map(String::valueOf)
				.collect(Collectors.collectingAndThen(Collectors.toList(), DrawableDAO::getDrawables));
	}

	public List<Drawable> getDeque() {
		return new JSONArray(deque).stream()
				.map(String::valueOf)
				.collect(Collectors.collectingAndThen(Collectors.toList(), DrawableDAO::getDrawables));
	}
}
