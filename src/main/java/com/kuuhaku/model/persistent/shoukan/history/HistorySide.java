/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.persistent.shoukan.history;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.common.shoukan.SlotColumn;
import com.kuuhaku.model.enums.shoukan.Lock;
import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.kuuhaku.model.persistent.converter.JSONObjectConverter;
import com.kuuhaku.model.records.id.HistorySideId;
import com.kuuhaku.model.records.shoukan.CardReference;
import com.ygimenez.json.JSONArray;
import com.ygimenez.json.JSONObject;
import jakarta.persistence.*;
import kotlin.Pair;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.*;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "history_side", schema = "shiro")
public class HistorySide extends DAO<HistorySide> {
	@EmbeddedId
	private HistorySideId id;

	@Column(name = "hp", nullable = false)
	private int hp;

	@Column(name = "mp", nullable = false)
	private int mp;

	@Column(name = "active_dot", nullable = false)
	private int activeDot;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "locks", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONObjectConverter.class)
	private JSONObject locks = new JSONObject();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "hand", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray hand = new JSONArray();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "deck", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray deck = new JSONArray();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "graveyard", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray graveyard = new JSONArray();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "discard", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray discard = new JSONArray();

	@OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumns({
			@JoinColumn(name = "match_id", referencedColumnName = "match_id"),
			@JoinColumn(name = "turn", referencedColumnName = "turn"),
			@JoinColumn(name = "side", referencedColumnName = "side")
	})
	@Fetch(FetchMode.SUBSELECT)
	private Set<HistorySlot> slots = new HashSet<>();

	public HistorySide() {
	}

	public HistorySide(HistoryTurn parent, Hand h) {
		this.id = new HistorySideId(parent.getId(), h.getSide());
		this.hp = h.getHP();
		this.mp = h.getMP();
		this.activeDot = h.getRegDeg().peek();

		for (Map.Entry<Lock, Integer> lock : h.getLocks()) {
			this.locks.put(lock.getKey().name(), lock.getValue());
		}

		List<Pair<List<Drawable<?>>, JSONArray>> stacks = List.of(
				new Pair<>(h.getCards(false), this.hand),
				new Pair<>(h.getRealDeck(false), this.deck),
				new Pair<>(h.getGraveyard(false), this.graveyard),
				new Pair<>(h.getDiscard(false), this.discard)
		);

		for (Pair<List<Drawable<?>>, JSONArray> stack : stacks) {
			for (Drawable<?> card : stack.getFirst()) {
				stack.getSecond().add(new CardReference(card));
			}
		}

		for (SlotColumn slot : h.getGame().getSlots(h.getSide())) {
			slots.add(new HistorySlot(this, slot.getTop(), slot.getBottom(), slot.getLock()));
		}
	}

	public HistorySide parent(HistoryTurn parent) {
		this.id = new HistorySideId(parent.getId(), id.side());

		for (HistorySlot slot : slots) {
			slot.parent(this);
		}

		return this;
	}

	public HistorySideId getId() {
		return id;
	}

	public int getHp() {
		return hp;
	}

	public int getMp() {
		return mp;
	}

	public int getActiveDot() {
		return activeDot;
	}

	public JSONObject getLocks() {
		return locks;
	}

	public JSONArray getHand() {
		return hand;
	}

	public JSONArray getDeck() {
		return deck;
	}

	public JSONArray getGraveyard() {
		return graveyard;
	}

	public Set<HistorySlot> getSlots() {
		return slots;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		HistorySide that = (HistorySide) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
