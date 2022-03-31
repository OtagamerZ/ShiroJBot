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

import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Field;
import com.kuuhaku.model.enums.DailyTask;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Deck;
import com.kuuhaku.utils.Constants;
import com.kuuhaku.utils.helpers.StringHelper;
import net.dv8tion.jda.api.entities.User;

import java.util.Map;

public class FieldDrop extends Drop<Field> {
	boolean mainPrize = true;

	public FieldDrop() {
		super(Field.getRandomField(false));
	}

	@Override
	public void award(User u) {
		Deck dk = KawaiponDAO.getDeck(u.getId());
		if (dk.getFields().size() < 3) {
			dk.addField(getPrize());
		} else {
			awardInstead(u, Constants.BASE_FIELD_PRICE);
			mainPrize = false;
			return;
		}
		dk.save();

		Account acc = Account.find(Account.class, u.getId());
		if (acc.hasPendingQuest()) {
			Map<DailyTask, Integer> pg = acc.getDailyProgress();
			pg.merge(DailyTask.DROP_TASK, 1, Integer::sum);
			acc.setDailyProgress(pg);
			acc.save();
		}
	}

	@Override
	public String toString() {
		return "Campo " + getPrize().getCard().getName();
	}

	@Override
	public String toString(User u) {
		if (mainPrize)
			return "Campo " + getPrize().getCard().getName();
		else
			return "~~Campo %s~~\n(convertido em %s CR)".formatted(
					getPrize().getCard().getName(),
					StringHelper.separate(Constants.BASE_FIELD_PRICE)
			);
	}
}
