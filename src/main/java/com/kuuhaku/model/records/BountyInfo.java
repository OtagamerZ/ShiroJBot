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

package com.kuuhaku.model.records;

import com.kuuhaku.controller.postgresql.BountyQuestDAO;
import com.kuuhaku.model.enums.Reward;
import com.kuuhaku.model.persistent.Attributes;

import java.util.Map;

public record BountyInfo(String id, int time, double diff, Attributes reqStats, Map<Reward, Integer> rewards) {
	@Override
	public String toString() {
		return BountyQuestDAO.getBounty(id).toString();
	}
}