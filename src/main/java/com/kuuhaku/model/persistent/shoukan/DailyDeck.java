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

package com.kuuhaku.model.persistent.shoukan;

import com.kuuhaku.controller.DAO;
import com.kuuhaku.model.persistent.user.Account;
import com.kuuhaku.model.records.shoukan.DeckEntry;
import com.kuuhaku.util.Calc;
import com.kuuhaku.util.Utils;

import java.util.Calendar;
import java.util.List;

public class DailyDeck extends Deck {
	public DailyDeck(Account account) {
		super(account);
	}

	@Override
	public String getName() {
		return "daily";
	}

	@Override
	public boolean isVariant() {
		Calendar cal = Calendar.getInstance();
		return Calc.rng(1d, (long) cal.get(Calendar.YEAR) + cal.get(Calendar.DAY_OF_YEAR)) > 0.5;
	}

	@Override
	public List<DeckEntry> getSenshiRaw() {
		return DAO.queryAllUnmapped("SELECT 'SENSHI', card_id, 0 FROM v_daily_senshi").stream()
				.map(o -> Utils.map(DeckEntry.class, o))
				.toList();
	}

	@Override
	public List<DeckEntry> getEvogearRaw() {
		return DAO.queryAllUnmapped("SELECT 'EVOGEAR', card_id, 0 FROM v_daily_evogear").stream()
				.map(o -> Utils.map(DeckEntry.class, o))
				.toList();
	}

	@Override
	public List<DeckEntry> getFieldsRaw() {
		return DAO.queryAllUnmapped("SELECT 'FIELD', card_id, 0 FROM v_daily_field").stream()
				.map(o -> Utils.map(DeckEntry.class, o))
				.toList();
	}
}
