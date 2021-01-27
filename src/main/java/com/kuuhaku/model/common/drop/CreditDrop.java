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

import com.github.twitch4j.common.events.domain.EventUser;
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.model.common.Consumable;
import com.kuuhaku.model.enums.DailyTask;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.function.Function;

public class CreditDrop extends Drop {
	private final int amount = 250 + Helper.rng(750, false);

	@Override
	public String getCaptcha() {
		return Helper.noCopyPaste(getRealCaptcha());
	}

	@Override
	public String getRealCaptcha() {
		try {
			return Hex.encodeHexString(MessageDigest.getInstance("MD5").digest(ByteBuffer.allocate(4).putInt(hashCode()).array())).substring(0, 5);
		} catch (NoSuchAlgorithmException e) {
			return String.valueOf(System.currentTimeMillis()).substring(0, 5);
		}
	}

	@Override
	public void award(User u) {
		Account acc = AccountDAO.getAccount(u.getId());
		acc.addCredit(amount, this.getClass());

		if (!acc.hasCompletedQuests()) {
			Map<DailyTask, Integer> pg = acc.getDailyProgress();
			pg.compute(DailyTask.DROP_TASK, (k, v) -> Helper.getOr(v, 0) + 1);
			acc.setDailyProgress(pg);
		}

		AccountDAO.saveAccount(acc);
	}

	@Override
	public void award(EventUser u) {

	}

	@Override
	public int getPrize() {
		return amount;
	}

	@Override
	public Consumable getPrizeAsItem() {
		return null;
	}

	@Override
	public String[] getPrizeWithPenalty() {
		return new String[0];
	}

	@Override
	public Pair<String, Function<User, Boolean>> getRequirement() {
		return getChosen();
	}

	@Override
	public Map.Entry<String, Function<EventUser, Boolean>> getRequirementForTwitch() {
		return null;
	}
}
