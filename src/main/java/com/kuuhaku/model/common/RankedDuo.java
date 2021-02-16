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

package com.kuuhaku.model.common;

import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.model.persistent.MatchMakingRating;
import net.dv8tion.jda.api.entities.User;

import java.util.Objects;

public class RankedDuo {
	private final MatchMakingRating p1;
	private final MatchMakingRating p2;

	public RankedDuo(User p1, User p2) {
		this.p1 = MatchMakingRatingDAO.getMMR(p1.getId());
		this.p2 = MatchMakingRatingDAO.getMMR(p2.getId());
	}

	public RankedDuo(MatchMakingRating p1, MatchMakingRating p2) {
		this.p1 = p1;
		this.p2 = p2;
	}

	public MatchMakingRating getP1() {
		return p1;
	}

	public MatchMakingRating getP2() {
		return p2;
	}

	public long getAvgMMR() {
		return (long) Math.ceil((p1.getMMR() + p2.getMMR()) / 2d);
	}

	public int getAvgTier() {
		return (int) (Math.ceil(p1.getTier().getTier() + p2.getTier().getTier()) / 2d);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RankedDuo rankedDuo = (RankedDuo) o;
		return Objects.equals(p1, rankedDuo.p1) && Objects.equals(p2, rankedDuo.p2);
	}

	@Override
	public int hashCode() {
		return Objects.hash(p1, p2);
	}
}
