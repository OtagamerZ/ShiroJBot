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

import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import org.json.JSONObject;

import javax.persistence.*;

@Entity
@Table(name = "matchround")
public class MatchRound {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Enumerated(value = EnumType.STRING)
	private Side side = Side.BOTTOM;

	@Column(columnDefinition = "TEXT")
	private String state = "{}";

	public int getId() {
		return id;
	}

	public JSONObject getScript() {
		return new JSONObject(state);
	}

	public void setScript(JSONObject state) {
		this.state = state.toString();
	}

	public Side getSide() {
		return side;
	}

	public void setSide(Side side) {
		this.side = side;
	}
}
