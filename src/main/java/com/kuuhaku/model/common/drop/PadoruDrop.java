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

package com.kuuhaku.model.common.drop;

import com.kuuhaku.model.enums.DailyTask;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.DynamicParameter;
import net.dv8tion.jda.api.entities.User;

import java.util.Map;

public class PadoruDrop extends Drop<Integer> {
	public PadoruDrop() {
		super(0);
	}

	@Override
	public void award(User u) {
		new DynamicParameter("padoru_" + u.getId(), "padoru").save();
		Account acc = Account.find(Account.class, u.getId());

		if (acc.hasPendingQuest()) {
			Map<DailyTask, Integer> pg = acc.getDailyProgress();
			pg.merge(DailyTask.DROP_TASK, 1, Integer::sum);
			acc.setDailyProgress(pg);
		}

		acc.save();
	}

	@Override
	public String toString() {
		return "Emblema padoru";
	}

	@Override
	public String toString(User u) {
		return toString();
	}
}
