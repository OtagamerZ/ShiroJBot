/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.model.records;

import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.model.persistent.MatchMakingRating;
import net.dv8tion.jda.api.entities.User;

public record RankedDuo(MatchMakingRating p1, MatchMakingRating p2) {

	public RankedDuo(User p1, User p2) {
		this(MatchMakingRatingDAO.getMMR(p1.getId()), MatchMakingRatingDAO.getMMR(p2.getId()));
	}

	public long getAvgMMR() {
		return (long) Math.ceil((p1.getMMR() + p2.getMMR()) / 2d);
	}

	public int getAvgTier() {
		return (int) (Math.ceil(p1.getTier().getTier() + p2.getTier().getTier()) / 2d);
	}
}
