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

import com.kuuhaku.model.common.shoukan.Hand;
import com.kuuhaku.model.enums.shoukan.Race;
import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.kuuhaku.model.records.id.HistoryPlayerId;
import com.kuuhaku.model.records.shoukan.Origin;
import com.ygimenez.json.JSONArray;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Objects;

@Entity
@Table(name = "history_player")
public class HistoryPlayer {
	@EmbeddedId
	private HistoryPlayerId id;

	@OneToOne
	@MapsId("matchId")
	private HistoryInfo parent;

	@Column(name = "hp", nullable = false)
	private int hp;

	@Column(name = "weight", nullable = false)
	private double weight;

	@Column(name = "divergence", nullable = false)
	private double divergence;

	@Enumerated(EnumType.STRING)
	@Column(name = "major_race", nullable = false)
	private Race majorRace;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "minor_races", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray minorRaces = new JSONArray();

	@Column(name = "using_variant", nullable = false)
	private boolean usingVariant;

	public HistoryPlayer() {
	}

	public HistoryPlayer(HistoryInfo parent, Hand hand) {
		this.id = new HistoryPlayerId(parent.getMatchId(), hand.getUid());
		this.parent = parent;
		this.hp = hand.getBase().hp();
		this.weight = hand.getUserDeck().getEvoWeight();
		this.divergence = hand.getUserDeck().getMetaDivergence();
		this.majorRace = hand.getOrigins().major();
		this.minorRaces = JSONArray.of((Object[]) hand.getOrigins().minor());
		this.usingVariant = hand.getOrigins().variant();
	}

	public HistoryPlayerId getId() {
		return id;
	}

	public int getHp() {
		return hp;
	}

	public double getWeight() {
		return weight;
	}

	public double getDivergence() {
		return divergence;
	}

	public Origin getOrigin() {
		return new Origin(usingVariant, majorRace, minorRaces.stream()
				.map(r -> Race.valueOf(String.valueOf(r)))
				.toArray(Race[]::new)
		);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		HistoryPlayer that = (HistoryPlayer) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
