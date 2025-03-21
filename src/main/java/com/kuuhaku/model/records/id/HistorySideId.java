/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2024  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.records.id;

import com.kuuhaku.model.enums.shoukan.Side;
import jakarta.persistence.*;

@Embeddable
public record HistorySideId(
		@Column(name = "match_id", nullable = false)
		int matchId,
		@Column(name = "turn", nullable = false)
		int turn,
		@Enumerated(EnumType.STRING)
		@Column(name = "side", nullable = false, columnDefinition = "VARCHAR(255)")
		Side side
) {
	public HistorySideId(HistoryTurnId parent, Side side) {
		this(parent.matchId(), parent.turn(), side);
	}
}