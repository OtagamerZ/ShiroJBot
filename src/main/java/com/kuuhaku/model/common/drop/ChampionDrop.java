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
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Champion;
import com.kuuhaku.model.enums.DailyTask;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.User;

import java.util.Map;

public class ChampionDrop extends Drop<Champion> {
	public ChampionDrop() {
		super(CardDAO.getRandomChampion());
	}

	@Override
	public void award(User u) {
		Kawaipon kp = KawaiponDAO.getKawaipon(u.getId());
		if (kp.getChampions().size() < 36 && kp.getChampionCopies(getPrize().getCard()) < kp.getChampionMaxCopies()) {
			kp.addChampion(getPrize());
		} else {
			awardInstead(u, getPrize().getCard().getRarity().getIndex() * Helper.BASE_CARD_PRICE);
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
		return "Campeão " + getPrize().getCard().getName();
	}

	@Override
	public String toString(User u) {
		Kawaipon kp = KawaiponDAO.getKawaipon(u.getId());
		if (kp.getChampions().size() < 36)
			return "Campeão " + getPrize().getCard().getName();
		else
			return "~~Campeão %s~~ (convertido em %s créditos)".formatted(
					getPrize().getCard().getName(),
					getPrize().getCard().getRarity().getIndex() * Helper.BASE_CARD_PRICE
			);
	}
}
