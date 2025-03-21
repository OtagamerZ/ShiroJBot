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
import com.kuuhaku.game.Shoukan;
import com.kuuhaku.interfaces.shoukan.Drawable;
import com.kuuhaku.model.enums.shoukan.Side;
import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.MatchHistory;
import com.kuuhaku.model.records.id.HistoryTurnId;
import com.ygimenez.json.JSONArray;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static jakarta.persistence.CascadeType.ALL;

@Entity
@Table(name = "history_turn", schema = "shiro")
public class HistoryTurn extends DAO<HistoryTurn> {
	@EmbeddedId
	private HistoryTurnId id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "match_id", nullable = false, updatable = false)
	@Fetch(FetchMode.JOIN)
	@MapsId("matchId")
	private MatchHistory parent;

	@OneToMany(cascade = ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumns({
			@JoinColumn(name = "match_id", referencedColumnName = "match_id"),
			@JoinColumn(name = "turn", referencedColumnName = "turn")
	})
	@Fetch(FetchMode.SUBSELECT)
	private Set<HistorySide> sides = new HashSet<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "banned", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray banned = new JSONArray();

	@ManyToOne(optional = false)
	@JoinColumn(name = "field_id")
	@Fetch(FetchMode.JOIN)
	private Card field;

	public HistoryTurn() {
	}

	public HistoryTurn(Shoukan game) {
		for (Side side : Side.values()) {
			sides.add(new HistorySide(this, game.getHands().get(side)));
		}
		this.banned = game.getArena().getBanned(false).stream()
				.map(Drawable::getId)
				.collect(Collectors.toCollection(JSONArray::new));
		this.field = game.getArena().getField().getCard();
	}

	public HistoryTurn parent(MatchHistory parent) {
		this.id = new HistoryTurnId(parent.getId(), parent.getTurns().size());
		this.parent = parent;

		for (HistorySide side : sides) {
			side.parent(this);
		}

		return this;
	}

	public HistoryTurnId getId() {
		return id;
	}

	public Set<HistorySide> getSides() {
		return sides;
	}

	public JSONArray getBanned() {
		return banned;
	}

	public Card getField() {
		return field;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		HistoryTurn that = (HistoryTurn) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
