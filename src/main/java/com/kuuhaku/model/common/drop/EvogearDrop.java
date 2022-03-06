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

import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.model.enums.DailyTask;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Deck;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.User;

import java.util.Map;

public class EvogearDrop extends Drop<Equipment> {
	boolean mainPrize = true;

	public EvogearDrop() {
		super(CardDAO.getRandomEquipment());
	}

	@Override
	public void award(User u) {
		Deck dk = KawaiponDAO.getDeck(u.getId());
		if (dk.getEvoWeight() + getPrize().getWeight(dk) <= 24 && dk.getEquipmentCopies(getPrize().getCard()) < dk.getEquipmentMaxCopies(getPrize())) {
			dk.addEquipment(getPrize());
		} else {
			awardInstead(u, Helper.BASE_EQUIPMENT_PRICE);
			mainPrize = false;
			return;
		}
		KawaiponDAO.saveDeck(dk);

		Account acc = AccountDAO.getAccount(u.getId());
		if (acc.hasPendingQuest()) {
			Map<DailyTask, Integer> pg = acc.getDailyProgress();
			pg.merge(DailyTask.DROP_TASK, 1, Integer::sum);
			acc.setDailyProgress(pg);
			AccountDAO.saveAccount(acc);
		}
	}

	@Override
	public String toString() {
		return "Evogear " + getPrize().getCard().getName();
	}

	@Override
	public String toString(User u) {
		if (mainPrize)
			return "Evogear " + getPrize().getCard().getName();
		else
			return "~~Evogear %s~~\n(convertido em %s CR)".formatted(
					getPrize().getCard().getName(),
					Helper.separate(Helper.BASE_EQUIPMENT_PRICE)
			);
	}
}
