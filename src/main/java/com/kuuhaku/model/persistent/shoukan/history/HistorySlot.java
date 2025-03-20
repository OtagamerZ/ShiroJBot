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

import com.kuuhaku.model.persistent.converter.JSONArrayConverter;
import com.kuuhaku.model.persistent.shiro.Card;
import com.kuuhaku.model.persistent.shoukan.Evogear;
import com.kuuhaku.model.persistent.shoukan.Senshi;
import com.kuuhaku.model.records.id.HistorySlotId;
import com.ygimenez.json.JSONArray;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Objects;
import java.util.stream.Collectors;

@Entity
@Table(name = "history_slot")
public class HistorySlot {
	@EmbeddedId
	private HistorySlotId id;

	@ManyToOne
	@Fetch(FetchMode.JOIN)
	@MapsId("sideId")
	private HistorySide parent;

	@ManyToOne
	@JoinColumn(name = "frontline_id")
	@Fetch(FetchMode.JOIN)
	private Card frontline;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "frontline_equips", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray frontlineEquips = new JSONArray();

	@ManyToOne
	@JoinColumn(name = "backline_id")
	@Fetch(FetchMode.JOIN)
	private Card backline;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "backline_equips", nullable = false, columnDefinition = "JSONB")
	@Convert(converter = JSONArrayConverter.class)
	private JSONArray backlineEquips = new JSONArray();

	@Column(name = "lock_time", nullable = false)
	private int lockTime;

	public HistorySlot() {
	}

	public HistorySlot(HistorySide parent, Senshi frontline, Senshi backline, int lockTime) {
		this.id = new HistorySlotId(parent.getId(), parent.getPlaced().size());
		this.parent = parent;

		if (frontline != null) {
			this.frontline = frontline.getCard();
			this.frontlineEquips = frontline.getEquipments(false).stream()
					.map(Evogear::getId)
					.collect(Collectors.toCollection(JSONArray::new));
		}

		if (backline != null) {
			this.backline = backline.getCard();
			this.backlineEquips = backline.getEquipments(false).stream()
					.map(Evogear::getId)
					.collect(Collectors.toCollection(JSONArray::new));
		}

		this.lockTime = lockTime;
	}

	public HistorySlotId getId() {
		return id;
	}

	public Card getFrontline() {
		return frontline;
	}

	public JSONArray getFrontlineEquips() {
		return frontlineEquips;
	}

	public Card getBackline() {
		return backline;
	}

	public JSONArray getBacklineEquips() {
		return backlineEquips;
	}

	public int getLockTime() {
		return lockTime;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		HistorySlot that = (HistorySlot) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
