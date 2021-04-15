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

package com.kuuhaku.model.common.drop;

import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.model.enums.DailyTask;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.User;

import java.util.Map;

public class EvogearDrop extends Drop<Equipment> {
	public EvogearDrop() {
		super(CardDAO.getRandomEquipment());
	}

	@Override
	public void award(User u) {
		Kawaipon kp = KawaiponDAO.getKawaipon(u.getId());
		if (kp.getEvoWeight() + getPrize().getWeight(kp) <= 24 && kp.getEquipmentCopies(getPrize().getCard()) < kp.getEquipmentMaxCopies(getPrize())) {
			kp.addEquipment(getPrize());
		} else {
			awardInstead(u, getPrize().getTier() * Helper.BASE_EQUIPMENT_PRICE);
			return;
		}
		KawaiponDAO.saveKawaipon(kp);

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
		return "evogear " + getPrize().getCard().getName();
	}

	@Override
	public String toString(User u) {
		Kawaipon kp = KawaiponDAO.getKawaipon(u.getId());
		if (kp.getEvoWeight() + getPrize().getWeight(kp) <= 24)
			return "evogear " + getPrize().getCard().getName();
		else
			return "~~evogear %s~~ (convertido em %s créditos)".formatted(
					getPrize().getCard().getName(),
					getPrize().getTier() * Helper.BASE_EQUIPMENT_PRICE
			);
	}
}
